package ledance.servicios.pago;

import jakarta.persistence.EntityExistsException;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final DetallePagoServicio detallePagoServicio;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;
    private final InscripcionMapper inscripcionMapper;
    private final StockServicio stockServicio;
    private final AlumnoMapper alumnoMapper;

    public PaymentProcessor(PagoRepositorio pagoRepositorio, AlumnoRepositorio alumnoRepositorio, InscripcionRepositorio inscripcionRepositorio, MetodoPagoRepositorio metodoPagoRepositorio, PagoMapper pagoMapper, MatriculaServicio matriculaServicio, DetallePagoMapper detallePagoMapper, MensualidadServicio mensualidadServicio, RecargoRepositorio recargoRepositorio, BonificacionRepositorio bonificacionRepositorio, DetallePagoServicio detallePagoServicio, PagoMedioMapper pagoMedioMapper, InscripcionMapper inscripcionMapper, StockServicio stockServicio, AlumnoMapper alumnoMapper) {
        this.pagoRepositorio = pagoRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadServicio = mensualidadServicio;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.pagoMedioMapper = pagoMedioMapper;
        this.inscripcionMapper = inscripcionMapper;
        this.stockServicio = stockServicio;
        this.alumnoMapper = alumnoMapper;
    }

    // **********************************************************************
    // Método para actualizar un pago histórico y, en caso de abono, crear un nuevo pago
    // **********************************************************************
    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        log.info("[actualizarCobranzaHistorica] Iniciando actualización histórica para pagoId={}", pagoId);

        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para actualización."));

        if (historico.getSaldoRestante() == 0) {
            throw new IllegalArgumentException("El pago ya está saldado y no se puede actualizar como histórico.");
        }
        log.info("[actualizarCobranzaHistorica] Pago histórico obtenido: id={}, saldoRestante={}", historico.getId(), historico.getSaldoRestante());

        double saldoHistoricoOriginal = historico.getSaldoRestante();
        log.info("[saldoHistoricoOriginal] Saldo histórico original: {}", saldoHistoricoOriginal);

        // Mapear abonos recibidos: clave = concepto en minúsculas y sin espacios extra.
        Map<String, DetallePagoRegistroRequest> nuevosAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(dto -> dto.concepto().trim().toLowerCase(), Function.identity()));
        log.info("[actualizarCobranzaHistorica] Abonos recibidos para conceptos: {}", nuevosAbonos.keySet());

        double sumaAbonoHistorico = 0.0;
        // Procesar cada detalle del histórico aplicando el abono correspondiente.
        for (DetallePago detalle : historico.getDetallePagos()) {
            String conceptoNormalizado = detalle.getConcepto().trim().toLowerCase();
            DetallePagoRegistroRequest dto = nuevosAbonos.get(conceptoNormalizado);
            if (dto == null) {
                throw new IllegalArgumentException("No se encontró abono para el concepto: " + detalle.getConcepto());
            }
            double abonoAplicado = Math.min(dto.aCobrar(), detalle.getImportePendiente());
            log.info("[actualizarCobranzaHistorica] Para concepto '{}': abonoAplicado={}, importePendiente anterior={}",
                    detalle.getConcepto(), abonoAplicado, detalle.getImportePendiente());
            sumaAbonoHistorico += abonoAplicado;
            double nuevoImportePendiente = detalle.getImportePendiente() - abonoAplicado;
            detalle.setaCobrar(abonoAplicado);
            detalle.setImportePendiente(nuevoImportePendiente);
            if (nuevoImportePendiente == 0.0) {
                detalle.setCobrado(true);
            }
            log.info("[actualizarCobranzaHistorica] Para concepto '{}': nuevo importePendiente={}", detalle.getConcepto(), detalle.getImportePendiente());

            // Determinar tipo de detalle sin reducir stock en actualización histórica.
            if (existeStockConNombre(conceptoNormalizado)) {
                detalle.setTipo(TipoDetallePago.STOCK);
                log.info("[actualizarCobranzaHistorica] Se asigna tipo STOCK para el concepto '{}'. No se reduce stock en flujo histórico.", detalle.getConcepto());
            } else if (conceptoNormalizado.startsWith("matricula")) {
                detalle.setTipo(TipoDetallePago.MATRICULA);
                log.info("[actualizarCobranzaHistorica] Se asigna tipo MATRICULA para el concepto '{}'", detalle.getConcepto());
                procesarMatricula(historico, detalle);
            } else if (conceptoNormalizado.contains("cuota")) {
                detalle.setTipo(TipoDetallePago.MENSUALIDAD);
                log.info("[actualizarCobranzaHistorica] Se asigna tipo MENSUALIDAD para el concepto '{}'", detalle.getConcepto());
                procesarMensualidad(historico, detalle);
            } else {
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                log.info("[actualizarCobranzaHistorica] Se asigna tipo CONCEPTO para el concepto '{}'", detalle.getConcepto());
            }
        }
        log.info("[actualizarCobranzaHistorica] Suma total de abonos aplicados: {}", sumaAbonoHistorico);

        // Actualizar el histórico marcándolo como saldado.
        historico.setSaldoRestante(0.0);
        historico.setEstadoPago(EstadoPago.HISTORICO);
        pagoRepositorio.save(historico);
        log.info("[actualizarCobranzaHistorica] Pago histórico actualizado: id={}, saldoRestante previo={} -> nuevo={}",
                historico.getId(), saldoHistoricoOriginal, historico.getSaldoRestante());

        // Calcular los valores para el nuevo pago.
        double nuevoPagoMonto = request.detallePagos().stream().mapToDouble(DetallePagoRegistroRequest::aCobrar).sum();
        // Nuevo saldo restante: saldo histórico original menos la suma de abonos aplicados.
        double nuevoPagoSaldoRestante = saldoHistoricoOriginal - sumaAbonoHistorico;
        if (nuevoPagoSaldoRestante < 0) {
            nuevoPagoSaldoRestante = 0.0;
        }
        log.info("[actualizarCobranzaHistorica] Nuevo pago: monto asignado={}, saldoRestante calculado={}", nuevoPagoMonto, nuevoPagoSaldoRestante);

        // Crear el nuevo pago a partir del histórico.
        Pago nuevoPago = crearNuevoPagoConResumen(request, historico, nuevoPagoMonto, nuevoPagoSaldoRestante);
        // Marcar el nuevo pago como RESUMEN.
        nuevoPago.setTipoPago(TipoPago.RESUMEN);
        log.info("[actualizarCobranzaHistorica] Nuevo pago creado: id={}, monto={}, saldoRestante={}", nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());

        return nuevoPago;
    }

    // Método auxiliar que crea un nuevo pago con un único detalle resumen
    @Transactional
    protected Pago crearNuevoPagoConResumen(PagoRegistroRequest request, Pago historico, double montoNuevo, double saldoNuevo) {
        log.info("[crearNuevoPagoConResumen] Iniciando creación de nuevo pago a partir del histórico id={}", historico.getId());

        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setRecargoAplicado(historico.getRecargoAplicado());
        nuevoPago.setBonificacionAplicada(historico.getBonificacionAplicada());
        nuevoPago.setTipoPago(TipoPago.RESUMEN); // Se marca como resumen.
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setSaldoAFavor(0.0);

        // Asignar el monto base y el monto final del nuevo pago.
        nuevoPago.setMontoBasePago(montoNuevo);
        nuevoPago.setMonto(montoNuevo);
        nuevoPago.setSaldoRestante(saldoNuevo);
        log.info("[crearNuevoPagoConResumen] Datos del nuevo pago -> MontoBasePago: {}, Monto: {}, SaldoRestante: {}",
                montoNuevo, montoNuevo, saldoNuevo);

        // Clonar cada detalle del histórico (manteniendo los importes ya actualizados).
        List<DetallePago> nuevosDetalles = new ArrayList<>();
        if (historico.getDetallePagos() != null && !historico.getDetallePagos().isEmpty()) {
            log.info("[crearNuevoPagoConResumen] Se encontraron {} detalle(s) en el pago histórico id={}", historico.getDetallePagos().size(), historico.getId());
            for (DetallePago detalleHistorico : historico.getDetallePagos()) {
                log.info("[crearNuevoPagoConResumen] Clonando detalle histórico id={} (concepto: '{}')", detalleHistorico.getId(), detalleHistorico.getConcepto());
                DetallePago nuevoDetalle = new DetallePago();

                nuevoDetalle.setCodigoConcepto(detalleHistorico.getCodigoConcepto());
                nuevoDetalle.setConcepto(detalleHistorico.getConcepto());
                nuevoDetalle.setCuota(detalleHistorico.getCuota());
                nuevoDetalle.setMontoOriginal(detalleHistorico.getMontoOriginal());
                nuevoDetalle.setImporteInicial(detalleHistorico.getImporteInicial());
                nuevoDetalle.setaCobrar(detalleHistorico.getaCobrar());
                nuevoDetalle.setImportePendiente(detalleHistorico.getImportePendiente());
                nuevoDetalle.setCobrado(detalleHistorico.getCobrado());
                nuevoDetalle.setBonificacion(detalleHistorico.getBonificacion());
                nuevoDetalle.setRecargo(detalleHistorico.getRecargo());
                nuevoDetalle.setTipo(detalleHistorico.getTipo());

                // Asociar el detalle al nuevo pago.
                nuevoDetalle.setPago(nuevoPago);
                nuevosDetalles.add(nuevoDetalle);

                log.info("[crearNuevoPagoConResumen] Detalle clonado: concepto='{}', aCobrar={}, importePendiente={}",
                        nuevoDetalle.getConcepto(), nuevoDetalle.getaCobrar(), nuevoDetalle.getImportePendiente());
            }
        } else {
            log.warn("[crearNuevoPagoConResumen] El pago histórico id={} no tiene detalles para clonar.", historico.getId());
        }

        nuevoPago.setDetallePagos(nuevosDetalles);

        try {
            nuevoPago = pagoRepositorio.save(nuevoPago);
            log.info("[crearNuevoPagoConResumen] Nuevo pago persistido: id={}, monto={}, saldoRestante={}",
                    nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());
        } catch (Exception ex) {
            log.error("[crearNuevoPagoConResumen] Error al persistir el nuevo pago: {}", ex.getMessage(), ex);
            throw ex;
        }

        return nuevoPago;
    }

    // **********************************************************************
    // Método para procesar el primer pago (cuando existe inscripción)
    // **********************************************************************
    @Transactional
    public Pago processFirstPayment(PagoRegistroRequest request, Inscripcion inscripcion) {
        log.info("[processFirstPayment] Iniciando primer pago para inscripción id={} y alumno id={}", inscripcion != null ? inscripcion.getId() : "N/A", request.alumno() != null ? request.alumno().id() : "N/A");

        Pago pago = pagoMapper.toEntity(request);

        pago.setInscripcion(inscripcion);
        pago.setAlumno(alumnoMapper.toEntity(request.alumno()));
        log.info("[processFirstPayment] Inscripción asignada al pago: {}", inscripcion);

        // 2. Procesar detalles...
        List<DetallePago> detalles = request.detallePagos().stream().map(detDTO -> {
            DetallePago det = detallePagoMapper.toEntity(detDTO);
            det.setPago(pago);

            double base = det.getMontoOriginal();
            double descuento = detallePagoServicio.calcularDescuento(det, base);
            double recargo = (det.getRecargo() != null) ? detallePagoServicio.obtenerValorRecargo(det, base) : 0.0;

            double importeInicial = base - descuento + recargo;
            det.setImporteInicial(importeInicial);

            double importePendiente = importeInicial - det.getaCobrar();
            det.setImportePendiente(Math.max(importePendiente, 0.0));

            String conceptoNormalizado = det.getConcepto().toUpperCase();
            if (existeStockConNombre(conceptoNormalizado)) {
                det.setTipo(TipoDetallePago.STOCK);
                det.setTipo(TipoDetallePago.STOCK);
            } else if (conceptoNormalizado.contains("CUOTA") || conceptoNormalizado.contains("CLASE SUELTA") || conceptoNormalizado.contains("CLASE DE PRUEBA")) {
                det.setTipo(TipoDetallePago.MENSUALIDAD);
            } else {
                det.setTipo(TipoDetallePago.CONCEPTO);
            }
            return det;
        }).collect(Collectors.toList());

        pago.setDetallePagos(detalles);

        // Asignar inscripción recuperada si es válida
        pago.setInscripcion(inscripcion);

        // 3. Calcular monto total del pago
        double montoTotal = detalles.stream().mapToDouble(DetallePago::getaCobrar).sum();
        pago.setMonto(montoTotal);
        pago.setMontoBasePago(montoTotal);

        log.info("[processFirstPayment] Monto total calculado: {}", montoTotal);

        // 4. Persistir pago
        Pago guardado = pagoRepositorio.save(pago);
        log.info("[processFirstPayment] Pago guardado inicialmente: id={}, monto={}", guardado.getId(), montoTotal);

        actualizarImportesPago(guardado);

        return guardado;
    }

    // Método que revisa los detalles de un pago y marca como COBRADO aquellos cuyos importes pendientes son 0.
    // Esto es útil para actualizar el estado de cada detalle después de aplicar abonos.
    private void marcarDetallesCobradosSiImporteEsCero(Pago pago) {
        log.info("[marcarDetallesCobradosSiImporteEsCero] Revisando detalles para pago id={}", pago.getId());
        if (pago.getDetallePagos() != null) {
            // Se recorre cada detalle del pago.
            pago.getDetallePagos().forEach(detalle -> {
                // Si el importe pendiente es 0, se marca el detalle como cobrado.
                if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                    detalle.setCobrado(true);
                    log.info("[marcarDetallesCobradosSiImporteEsCero] Detalle id={} marcado como COBRADO (importePendiente=0).", detalle.getId());
                } else {
                    // Se registra el estado actual del importe pendiente para el detalle.
                    log.info("[marcarDetallesCobradosSiImporteEsCero] Detalle id={} con importePendiente={}", detalle.getId(), detalle.getImportePendiente());
                }
            });
        } else {
            // Si no existen detalles asociados al pago, se genera una advertencia.
            log.warn("[marcarDetallesCobradosSiImporteEsCero] Pago id={} no tiene detalles.", pago.getId());
        }
        // Se guarda el estado actualizado del pago en la base de datos.
        pagoRepositorio.save(pago);
    }

    // Método para crear un pago base a partir de los datos del request.
    // Este método inicializa los campos principales del pago (fecha, monto, alumno, inscripcion, etc.)
    // sin incluir aún los detalles del pago, los cuales se procesarán en métodos posteriores.
    private Pago crearPagoBase(PagoRegistroRequest request) {
        log.info("[crearPagoBase] Iniciando creación de pago base");
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setAlumno(alumnoMapper.toEntity(request.alumno()));

        // Mapear la inscripción
        Inscripcion inscripcion = inscripcionMapper.toEntity(request.inscripcion());
        log.info("[crearPagoBase] Inscripción mapeada inicialmente: {}", inscripcion);
        if (inscripcion == null || inscripcion.getId() == null || inscripcion.getId() <= 0) {
            log.info("[crearPagoBase] Inscripción inválida detectada (id: {}), asignando null", (inscripcion != null ? inscripcion.getId() : "N/A"));
            pago.setInscripcion(null);
        } else {
            pago.setInscripcion(inscripcion);
        }

        // El saldo inicial es el monto total del pago.
        pago.setSaldoRestante(request.monto());
        pago.setSaldoAFavor(0.0);
        pago.setEstadoPago(EstadoPago.ACTIVO);

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId()).orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
            log.info("[crearPagoBase] Método de pago asignado: {}", metodo.getDescripcion());
        }

        // Opcional: Si quieres que el pago también tenga un "monto base" para fines de cálculo,
        // puedes asignarlo inicialmente igual al monto total.
        pago.setMontoBasePago(request.monto());

        log.info("[crearPagoBase] Pago base creado: {}", pago);
        return pago;
    }

    // Actualiza los campos comunes de un pago existente
    private void actualizarCamposPago(Pago pago, PagoRegistroRequest request, Alumno alumno, Inscripcion inscripcion) {
        log.info("[actualizarCamposPago] Actualizando datos principales para pago id={}", pago.getId());
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pago.setAlumno(alumno);
        pago.setInscripcion(inscripcion);
        pago.setEstadoPago(EstadoPago.ACTIVO);  // Nuevo pago activo
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId()).orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        pago.setMonto(request.monto());
        pago.setSaldoAFavor(0.0);
    }

    // **********************************************************************
    // Método para actualizar los importes totales del pago (saldo, montoPagado, etc.)
    // **********************************************************************
    @Transactional
    public void actualizarImportesPago(Pago pago) {
        log.info("[actualizarImportesPago] Iniciando actualización de importes para pago id={}", pago.getId());

        List<DetallePago> detalles = pago.getDetallePagos();
        if (detalles == null || detalles.isEmpty()) {
            log.warn("[actualizarImportesPago] No se encontraron detalles para el pago id={}. Se establecerán importes en 0.", pago.getId());
            pago.setMontoPagado(0.0);
            pago.setSaldoRestante(0.0);
            try {
                pagoRepositorio.save(pago);
                log.info("[actualizarImportesPago] Pago id={} actualizado sin detalles.", pago.getId());
            } catch (Exception ex) {
                log.error("[actualizarImportesPago] Error al persistir pago id={} sin detalles: {}", pago.getId(), ex.getMessage(), ex);
            }
            return;
        }

        boolean esResumen = isPagoResumen(pago);
        log.info("[actualizarImportesPago] Pago id={} ¿es resumen?: {}", pago.getId(), esResumen);

        double totalInicial = 0.0;
        double totalPendiente = 0.0;

        if (!esResumen) {
            log.info("[actualizarImportesPago] Procesando pago REGULAR con {} detalle(s) para el pago id={}", detalles.size(), pago.getId());
            for (DetallePago detalle : detalles) {
                log.info("[actualizarImportesPago] Procesando detalle id={} (concepto: '{}')", detalle.getId(), detalle.getConcepto());
                // Se recalcula el importe solo en flujo normal.
                detallePagoServicio.calcularImporte(detalle);
                // Se actualizan (reinicializan) según el monto original y lo cobrado.
                double nuevoImporteInicial = detalle.getMontoOriginal();
                double nuevoImportePendiente = nuevoImporteInicial - detalle.getaCobrar();
                detalle.setImporteInicial(nuevoImporteInicial);
                detalle.setImportePendiente(nuevoImportePendiente);
                log.info("[actualizarImportesPago] Detalle id={} -> ImporteInicial: {}, ImportePendiente: {}", detalle.getId(), nuevoImporteInicial, nuevoImportePendiente);
                totalInicial += nuevoImporteInicial;
                totalPendiente += nuevoImportePendiente;
            }
        } else {
            log.info("[actualizarImportesPago] Procesando pago RESUMEN para el pago id={}", pago.getId());
            for (DetallePago detalle : detalles) {
                double impInicial = (detalle.getImporteInicial() != null) ? detalle.getImporteInicial() : 0.0;
                double impPendiente = (detalle.getImportePendiente() != null) ? detalle.getImportePendiente() : 0.0;
                log.info("[actualizarImportesPago] Detalle id={} -> ImporteInicial: {}, ImportePendiente: {}", detalle.getId(), impInicial, impPendiente);
                totalInicial += impInicial;
                totalPendiente += impPendiente;
            }
        }

        log.info("[actualizarImportesPago] Total Inicial acumulado: {}", totalInicial);
        log.info("[actualizarImportesPago] Total Pendiente acumulado: {}", totalPendiente);

        pago.setSaldoRestante(totalPendiente);
        double montoPagado = totalInicial - totalPendiente;
        pago.setMontoPagado(montoPagado);
        log.info("[actualizarImportesPago] MontoPagado calculado (TotalInicial - TotalPendiente): {}", montoPagado);

        verificarSaldoRestante(pago);

        try {
            pagoRepositorio.save(pago);
            log.info("[actualizarImportesPago] Pago id={} guardado correctamente. MontoPagado={}, SaldoRestante={}",
                    pago.getId(), pago.getMontoPagado(), pago.getSaldoRestante());
        } catch (Exception ex) {
            log.error("[actualizarImportesPago] Error al persistir el pago id={}: {}", pago.getId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Método auxiliar que determina si el pago es de resumen.
     * Se considera resumen si el pago tiene un único detalle cuyo código de concepto es "RESUMEN".
     */
    private boolean isPagoResumen(Pago pago) {
        List<DetallePago> detalles = pago.getDetallePagos();
        if (detalles != null && detalles.size() == 1) {
            DetallePago detalle = detalles.get(0);
            if ("RESUMEN".equalsIgnoreCase(detalle.getCodigoConcepto())) {
                log.info("[isPagoResumen] Pago id={} es de resumen porque su único detalle tiene código 'RESUMEN'", pago.getId());
                return true;
            }
        }
        return false;
    }

    // **********************************************************************
    // Método para procesar pagos específicos según el concepto del detalle
    // (MATRICULA, MENSUALIDAD, STOCK O CONCEPTO)
    // **********************************************************************
    public void procesarPagosEspecificos(Pago pago) {
        log.info("[procesarPagosEspecificos] Iniciando procesamiento de detalles para pago id={}", pago.getId());
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                if (detalle.getTipo() == null) {
                    if (detalle.getConcepto() != null) {
                        String conceptoOriginal = detalle.getConcepto();
                        String conceptoNormalizado = conceptoOriginal.trim().toUpperCase();
                        log.info("[procesarPagosEspecificos] Concepto recibido: '{}', normalizado a: '{}'", conceptoOriginal, conceptoNormalizado);

                        if (existeStockConNombre(detalle.getConcepto())) {
                            detalle.setTipo(TipoDetallePago.STOCK);
                            log.info("[procesarPagosEspecificos] Se asigna tipo STOCK para el concepto '{}'", conceptoOriginal);
                        } else if (conceptoNormalizado.startsWith("MATRICULA")) {
                            detalle.setTipo(TipoDetallePago.MATRICULA);
                            log.info("[procesarPagosEspecificos] Se asigna tipo MATRICULA para el concepto '{}'", conceptoOriginal);
                            procesarMatricula(pago, detalle);
                        } else if (conceptoNormalizado.contains("CUOTA") || conceptoNormalizado.contains("CLASE SUELTA") || conceptoNormalizado.contains("CLASE DE PRUEBA")) {
                            detalle.setTipo(TipoDetallePago.MENSUALIDAD);
                            log.info("[procesarPagosEspecificos] Se asigna tipo MENSUALIDAD para el concepto '{}'", conceptoOriginal);
                            procesarMensualidad(pago, detalle);
                        } else {
                            // Aquí se corrige el error asignando explícitamente un valor por defecto.
                            detalle.setTipo(TipoDetallePago.CONCEPTO);
                            log.info("[procesarPagosEspecificos] Se asigna tipo CONCEPTO para el concepto '{}'", conceptoOriginal);
                        }
                    } else {
                        log.warn("[procesarPagosEspecificos] Detalle id={} sin concepto; se omite", detalle.getId());
                    }
                } else {
                    // Procesar según el tipo definido
                    switch (detalle.getTipo()) {
                        case MATRICULA:
                            log.info("[procesarPagosEspecificos] Procesando MATRICULA para detalle id={}", detalle.getId());
                            procesarMatricula(pago, detalle);
                            break;
                        case MENSUALIDAD:
                            log.info("[procesarPagosEspecificos] Procesando MENSUALIDAD para detalle id={}", detalle.getId());
                            procesarMensualidad(pago, detalle);
                            break;
                        case STOCK:
                            log.info("[procesarPagosEspecificos] Procesando STOCK para detalle id={}", detalle.getId());
                            break;
                        case CONCEPTO:
                            log.info("[procesarPagosEspecificos] Procesando CONCEPTO para detalle id={}", detalle.getId());
                            break;
                    }
                }
            }
        } else {
            log.warn("[procesarPagosEspecificos] El pago id={} no contiene detalles", pago.getId());
        }
        log.info("[procesarPagosEspecificos] Finalizado procesamiento de detalles para pago id={}", pago.getId());
    }

    private void procesarStock(DetallePago detalle) {
        log.info("[procesarStock] Iniciando procesamiento para detalle id={} con concepto '{}'", detalle.getId(), detalle.getConcepto());

        int cantidad = 1;
        if (detalle.getCuota() != null) {
            try {
                cantidad = Integer.parseInt(detalle.getCuota().trim());
            } catch (NumberFormatException e) {
                log.warn("[procesarStock] No se pudo parsear cuota '{}'. Usando cantidad 1.", detalle.getCuota());
            }
        }

        String nombreProducto = detalle.getConcepto().trim();
        log.info("[procesarStock] Nombre del producto para búsqueda: '{}'", nombreProducto);
        try {
            stockServicio.reducirStock(nombreProducto, cantidad);
            log.info("[procesarStock] Stock actualizado para '{}'. Se redujo la cantidad en {}", nombreProducto, cantidad);
        } catch (IllegalArgumentException ex) {
            log.error("[procesarStock] Error al reducir stock para '{}': {}", nombreProducto, ex.getMessage());
        }
    }

    private boolean existeStockConNombre(String nombre) {
        String nombreNormalizado = nombre.trim();
        log.info("[existeStockConNombre] Buscando stock con nombre normalizado: '{}'", nombreNormalizado);
        StockResponse stockResponse = stockServicio.obtenerStockPorNombre(nombreNormalizado);
        boolean existe = (stockResponse != null);
        log.info("[existeStockConNombre] Stock encontrado: {}, existe: {}", stockResponse, existe);
        return existe;
    }

    // Procesamiento para matrícula (ejemplo sin cambios significativos)
    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matrícula para detalle id={}", detalle.getId());
        if (pago.getAlumno() != null) {
            String[] partes = detalle.getConcepto().split(" ");
            log.info("[procesarMatricula] Partes del concepto: {}", Arrays.toString(partes));
            if (partes.length >= 2) {
                try {
                    int anio = Integer.parseInt(partes[1]);
                    log.info("[procesarMatricula] Procesando matrícula para el año {}", anio);
                    MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                    log.info("[procesarMatricula] Matrícula pendiente encontrada: {}", matResp);
                    MatriculaModificacionRequest modRequest = new MatriculaModificacionRequest(matResp.anio(), true, pago.getFecha());
                    matriculaServicio.actualizarEstadoMatricula(matResp.id(), modRequest);
                } catch (NumberFormatException e) {
                    log.warn("[procesarMatricula] Error al extraer el año del concepto '{}': {}", detalle.getConcepto(), e.getMessage());
                }
            } else {
                log.warn("[procesarMatricula] El concepto '{}' no tiene el formato esperado para matrícula", detalle.getConcepto());
            }
        } else {
            log.warn("[procesarMatricula] No se pudo procesar matrícula, pago sin alumno asignado.");
        }
        log.info("[procesarMatricula] Finalizado procesamiento de matrícula para detalle id={}", detalle.getId());
    }

    // Procesamiento para mensualidad
    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        log.info("[procesarMensualidad] Iniciando procesamiento de mensualidad para detalle id={}", detalle.getId());
        if (pago.getInscripcion() != null) {
            String descripcionDetalle = detalle.getConcepto().trim();
            MensualidadResponse mensPendiente = mensualidadServicio.buscarMensualidadPendientePorDescripcion(pago.getInscripcion(), descripcionDetalle);
            if (mensPendiente != null) {
                double montoAbonadoAcumulado = pago.getDetallePagos().stream().filter(d -> d.getConcepto().trim().equalsIgnoreCase(descripcionDetalle)).mapToDouble(DetallePago::getaCobrar).sum();
                log.info("[procesarMensualidad] Mensualidad id={} ('{}'): monto abonado acumulado={} y total a pagar={}", mensPendiente.id(), descripcionDetalle, montoAbonadoAcumulado, mensPendiente.totalPagar());
                if (montoAbonadoAcumulado >= mensPendiente.totalPagar()) {
                    log.info("[procesarMensualidad] Condicion cumplida: {} >= {}. Marcando mensualidad id={} como PAGADO.", montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.marcarComoPagada(mensPendiente.id(), pago.getFecha());
                } else {
                    log.info("[procesarMensualidad] Abono parcial: {} < {}. Actualizando abono parcial para mensualidad id={}.", montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.actualizarAbonoParcial(mensPendiente.id(), montoAbonadoAcumulado);
                }
            } else {
                log.info("[procesarMensualidad] No se encontro mensualidad pendiente para '{}'. Se creará una nueva.", descripcionDetalle);
                mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), descripcionDetalle, pago.getFecha());
            }
        }
    }

    private Pago obtenerPagoHistorico(Long pagoId) {
        Pago historico = pagoRepositorio.findById(pagoId).orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para actualizacion."));
        log.info("[obtenerPagoHistorico] Pago obtenido: id={}, saldoRestante={}", historico.getId(), historico.getSaldoRestante());
        if (historico.getSaldoRestante() == 0) {
            log.error("[obtenerPagoHistorico] El pago id={} tiene saldoRestante=0, no se puede actualizar como histórico.", historico.getId());
            throw new IllegalArgumentException("El pago ya está saldado y no se puede actualizar como historico.");
        }
        return historico;
    }

    private Map<String, DetallePagoRegistroRequest> mapearNuevosAbonos(PagoRegistroRequest request) {
        return request.detallePagos().stream().collect(Collectors.toMap(dto -> dto.concepto().trim().toLowerCase(), Function.identity()));
    }

    // **********************************************************************
    // Método para validar si el pago histórico es aplicable para ser actualizado.
    // Se verifica que para cada detalle enviado en el request exista un detalle en el pago histórico
    // que tenga el mismo concepto (ignorando mayúsculas y espacios).
    // **********************************************************************
    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null) {
            log.info("[esPagoHistoricoAplicable] Pago histórico es nulo, retornando false.");
            return false;
        }
        boolean aplicable = request.detallePagos().stream().allMatch(dto -> {
            String conceptoRequest = dto.concepto().trim().toLowerCase();
            boolean existe = ultimoPendiente.getDetallePagos().stream().anyMatch(det -> {
                String conceptoHistorico = det.getConcepto().trim().toLowerCase();
                boolean coincide = conceptoHistorico.equals(conceptoRequest);
                log.info("[esPagoHistoricoAplicable] Comparando request concepto '{}' con histórico concepto '{}': {}", conceptoRequest, conceptoHistorico, coincide);
                return coincide;
            });
            log.info("[esPagoHistoricoAplicable] Para el concepto '{}' del request, ¿existe en histórico? {}", conceptoRequest, existe);
            return existe;
        });
        log.info("[esPagoHistoricoAplicable] Resultado final: {}", aplicable);
        return aplicable;
    }

    // **********************************************************************
    // Método para decidir el flujo de pago según si hay inscripción y si se puede actualizar un histórico
    // **********************************************************************
    public Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        log.info("[crearPagoSegunInscripcion] Procesando pago para inscripción: {}", request.inscripcion() != null ? request.inscripcion().id() : "N/A");

        // Convertir el alumno del request
        Alumno alumno = alumnoMapper.toEntity(request.alumno());

        // Verificar la inscripción: si el id es válido (> 0), se mapea; de lo contrario, se deja null.
        Inscripcion inscripcion = null;
        // Corregido así:
        if (request.inscripcion() != null && request.inscripcion().id() != null && request.inscripcion().id() > 0) {
            inscripcion = inscripcionRepositorio.findById(request.inscripcion().id()).orElseThrow(() -> new EntityExistsException("Inscripción no encontrada con id=" + request.inscripcion().id()));
        }


        // Luego sigue la lógica para obtener el último pago pendiente y decidir el flujo:
        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (ultimoPendiente != null) {
            log.info("[crearPagoSegunInscripcion] Último pago pendiente: id={}, saldoRestante={}", ultimoPendiente.getId(), ultimoPendiente.getSaldoRestante());
        }

        if (ultimoPendiente != null && ultimoPendiente.getSaldoRestante() > 0 && esPagoHistoricoAplicable(ultimoPendiente, request)) {
            log.info("[crearPagoSegunInscripcion] Se detectó pago histórico aplicable; se actualizará.");
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            if (ultimoPendiente != null && ultimoPendiente.getSaldoRestante() == 0) {
                log.info("[crearPagoSegunInscripcion] Pago histórico saldado; se creará un nuevo pago.");
            } else {
                log.info("[crearPagoSegunInscripcion] No se cumple condición de histórico; se procesa un nuevo pago.");
            }

            // Si la inscripción es nula (lo cual es válido para pagos que no requieren inscripción)
            // se procesa el pago sin vincular inscripción.
            return processFirstPayment(request, inscripcion); // processFirstPayment debe manejar correctamente el caso en que inscripcion sea null.
        }
    }

    // **********************************************************************
    // Método para obtener el último pago pendiente (activo) de un alumno.
    // Se asume que el repositorio filtra por estado ACTIVO.
    // **********************************************************************
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        Optional<Pago> optPago = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0);
        if (optPago.isPresent()) {
            Pago pago = optPago.get();
            log.info("[obtenerUltimoPagoPendienteEntidad] Se encontró pago pendiente: id={}, saldoRestante={}", pago.getId(), pago.getSaldoRestante());
            return pago;
        } else {
            log.info("[obtenerUltimoPagoPendienteEntidad] No se encontró pago pendiente para alumnoId={}", alumnoId);
            return null;
        }
    }

    private void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.", pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}", pago.getId(), pago.getSaldoRestante());
        }
    }
}
