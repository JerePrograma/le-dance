package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            AlumnoRepositorio alumnoRepositorio,
                            InscripcionRepositorio inscripcionRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PagoMapper pagoMapper,
                            MatriculaServicio matriculaServicio,
                            DetallePagoMapper detallePagoMapper,
                            MensualidadServicio mensualidadServicio,
                            RecargoRepositorio recargoRepositorio,
                            BonificacionRepositorio bonificacionRepositorio,
                            DetallePagoServicio detallePagoServicio,
                            PagoMedioMapper pagoMedioMapper,
                            InscripcionMapper inscripcionMapper) {
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
    }

    // ---------------------- Creacion de Pagos -----------------------

    // Método refactorizado para procesar un pago GENERAL (sin inscripcion)
    // Ahora, antes de crear un pago nuevo, se verifica si existe un pago historico pendiente aplicable.
    // Si se detecta, se delega a actualizarCobranzaHistorica para cerrar el historico y generar uno nuevo.
    // Método para procesar un pago GENERAL (sin inscripción)
    public Pago processGeneralPayment(PagoRegistroRequest request) {
        log.info("[processGeneralPayment] Procesando pago GENERAL (sin inscripción).");

        // 1. Obtener el alumno a partir del request.
        Long alumnoId = request.alumno().id();
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado para pago general."));
        log.debug("[processGeneralPayment] Alumno obtenido: id={}", alumnoId);

        // 2. Verificar nuevamente si existe un pago histórico aplicable.
        Pago ultimoHistorico = obtenerUltimoPagoPendienteEntidad(alumnoId);
        if (ultimoHistorico != null && ultimoHistorico.getSaldoRestante() > 0
                && esPagoHistoricoAplicable(ultimoHistorico, request)) {
            log.info("[processGeneralPayment] Se detectó pago histórico aplicable; delegando a actualizarCobranzaHistorica.");
            return actualizarCobranzaHistorica(ultimoHistorico.getId(), request);
        }

        // 3. Crear la base del nuevo pago GENERAL.
        Pago pago = crearPagoBaseGeneral(request, alumno);
        List<DetallePago> detallesNuevos = obtenerDetallesNuevos(request, pago);
        log.debug("[processGeneralPayment] Detalles nuevos obtenidos: {}", detallesNuevos.size());

        // 4. Procesar cada detalle: asignar aCobrar y calcular el importe pendiente.
        for (DetallePago det : detallesNuevos) {
            double abono = (det.getaCobrar() != null && det.getaCobrar() > 0)
                    ? det.getaCobrar() : det.getValorBase();
            if (abono > det.getImporteInicial()) {
                abono = det.getImporteInicial();
            }
            det.setaCobrar(abono);
            double pendiente = det.getImporteInicial() - abono;
            det.setImportePendiente(pendiente);
            det.setCobrado(pendiente == 0.0);
            log.debug("[processGeneralPayment] Detalle id={} | aCobrar={}, ImporteInicial={}, ImportePendiente={}, Cobrado={}",
                    det.getId(), abono, det.getImporteInicial(), pendiente, pendiente == 0.0);
        }
        pago.setDetallePagos(detallesNuevos);

        // 5. Calcular monto total y saldoRestante del pago.
        double montoTotal = detallesNuevos.stream().mapToDouble(DetallePago::getaCobrar).sum();
        pago.setMonto(montoTotal);

        double saldoRestante = detallesNuevos.stream().mapToDouble(DetallePago::getImportePendiente).sum();
        pago.setSaldoRestante(saldoRestante);
        pago.setTipoPago(TipoPago.GENERAL);
        log.info("[processGeneralPayment] Pago calculado: montoTotal={}, saldoRestante={}", montoTotal, saldoRestante);

        // 6. Persistir el pago y actualizar importes.
        Pago guardado = pagoRepositorio.save(pago);
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);
        log.info("[processGeneralPayment] Pago GENERAL procesado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());

        return guardado;
    }

    // **********************************************************************
    // Método auxiliar para crear la base de un pago GENERAL (sin inscripción)
    // **********************************************************************
    private Pago crearPagoBaseGeneral(PagoRegistroRequest request, Alumno alumno) {
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pago.setEstadoPago(EstadoPago.ACTIVO);
        pago.setInscripcion(null); // Pago general sin inscripción
        pago.setTipoPago(TipoPago.GENERAL);
        pago.setAlumno(alumno);
        pago.setSaldoAFavor(0.0);
        // Si se especifica un método de pago, se asigna.
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        return pago;
    }

    // **********************************************************************
    // Método para actualizar un pago histórico y, en caso de abono, crear un nuevo pago
    // **********************************************************************
    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        log.info("[actualizarCobranzaHistorica] Iniciando actualización histórica para pagoId={}", pagoId);

        // 1. Recuperar y validar el pago histórico.
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para actualización."));
        if (historico.getSaldoRestante() == 0) {
            throw new IllegalArgumentException("El pago ya está saldado y no se puede actualizar como histórico.");
        }
        log.debug("[actualizarCobranzaHistorica] Pago histórico obtenido: id={}, saldoRestante={}",
                historico.getId(), historico.getSaldoRestante());

        // Guardar el saldo original para los cálculos.
        double saldoHistoricoOriginal = historico.getSaldoRestante();

        // 2. Mapear los abonos del request por concepto (normalizados).
        Map<String, DetallePagoRegistroRequest> nuevosAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(dto -> dto.concepto().trim().toLowerCase(), Function.identity()));
        log.debug("[actualizarCobranzaHistorica] Abonos recibidos: {}", nuevosAbonos.keySet());

        double sumaAbonoHistorico = 0.0;
        // 3. Procesar cada detalle del histórico aplicando los abonos.
        for (DetallePago detalle : historico.getDetallePagos()) {
            String key = detalle.getConcepto().trim().toLowerCase();
            DetallePagoRegistroRequest dto = nuevosAbonos.get(key);
            if (dto == null) {
                throw new IllegalArgumentException("No se encontró abono para el concepto: " + detalle.getConcepto());
            }
            double abonoAplicado = Math.min(dto.aCobrar(), detalle.getImportePendiente());
            log.debug("[actualizarCobranzaHistorica] Detalle '{}': abonoAplicado={}, importePendiente anterior={}",
                    detalle.getConcepto(), abonoAplicado, detalle.getImportePendiente());
            sumaAbonoHistorico += abonoAplicado;
            double nuevoImportePendiente = detalle.getImportePendiente() - abonoAplicado;
            detalle.setaCobrar(abonoAplicado);
            detalle.setImportePendiente(nuevoImportePendiente);
            if (nuevoImportePendiente == 0.0) {
                detalle.setCobrado(true);
            }
            log.debug("[actualizarCobranzaHistorica] Detalle '{}': nuevo importePendiente={}",
                    detalle.getConcepto(), detalle.getImportePendiente());
        }
        log.info("[actualizarCobranzaHistorica] Suma total de abonos aplicados: {}", sumaAbonoHistorico);

        // 4. Actualizar el pago histórico: reducir su saldoRestante sin modificar el monto.
        double nuevoSaldoHistorico = saldoHistoricoOriginal - sumaAbonoHistorico;
        historico.setSaldoRestante(nuevoSaldoHistorico);
        pagoRepositorio.save(historico);
        log.info("[actualizarCobranzaHistorica] Pago histórico actualizado: id={}, monto={}, saldoRestante previo={} -> nuevo={}",
                historico.getId(), historico.getMonto(), saldoHistoricoOriginal, historico.getSaldoRestante());

        // 5. Definir los valores para el nuevo pago:
        // Se asume que request.monto() contiene la suma de los aCobrar del nuevo pago.
        double nuevoPagoMonto = request.monto();
        double nuevoPagoSaldoRestante = saldoHistoricoOriginal - nuevoPagoMonto;
        if (nuevoPagoSaldoRestante < 0) {
            nuevoPagoSaldoRestante = 0.0;
        }
        log.info("[actualizarCobranzaHistorica] Nuevo pago: monto asignado={}, saldoRestante calculado={}",
                nuevoPagoMonto, nuevoPagoSaldoRestante);

        // 6. Crear el nuevo pago con los datos relevantes del histórico.
        Pago nuevoPago = crearNuevoPagoConResumen(request, historico, nuevoPagoMonto, nuevoPagoSaldoRestante);
        log.info("[actualizarCobranzaHistorica] Nuevo pago creado: id={}, monto={}, saldoRestante={}",
                nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());

        return nuevoPago;
    }

    // Método auxiliar que crea un nuevo pago con un único detalle resumen
    private Pago crearNuevoPagoConResumen(PagoRegistroRequest request, Pago historico, double montoNuevo, double saldoNuevo) {
        log.info("[crearNuevoPagoConResumen] Creando nuevo pago a partir del histórico id={}", historico.getId());

        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setRecargoAplicado(historico.getRecargoAplicado());
        nuevoPago.setBonificacionAplicada(historico.getBonificacionAplicada());
        nuevoPago.setTipoPago(TipoPago.GENERAL);
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setSaldoAFavor(0.0);

        // Crear un detalle resumen que refleje el abono recibido para el nuevo pago.
        DetallePago detalleNuevo = new DetallePago();
        detalleNuevo.setCodigoConcepto("RESUMEN");
        detalleNuevo.setConcepto("Abono nuevo pago");
        detalleNuevo.setValorBase(montoNuevo);
        detalleNuevo.setImporteInicial(montoNuevo);
        detalleNuevo.setaCobrar(montoNuevo);
        detalleNuevo.setImportePendiente(0.0);
        detalleNuevo.setCobrado(true);
        detalleNuevo.setTipo(TipoDetallePago.GENERAL);
        detalleNuevo.setPago(nuevoPago);

        nuevoPago.setDetallePagos(new ArrayList<>());
        nuevoPago.getDetallePagos().add(detalleNuevo);

        // Asignar monto y saldoRestante calculados.
        nuevoPago.setMonto(montoNuevo);
        nuevoPago.setSaldoRestante(saldoNuevo);

        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[crearNuevoPagoConResumen] Nuevo pago persistido: id={}, monto={}, saldoRestante={}",
                nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());

        return nuevoPago;
    }

    // **********************************************************************
    // Método auxiliar para clonar un detalle del pago histórico para el nuevo pago
    // En el nuevo detalle, el importe inicial y aCobrar se fijan al monto abonado.
    // **********************************************************************
    private DetallePago clonarDetalleParaNuevoPago(DetallePago original, double nuevoAbono) {
        DetallePago nuevo = new DetallePago();
        nuevo.setCodigoConcepto(original.getCodigoConcepto());
        nuevo.setConcepto(original.getConcepto());
        nuevo.setCuota(original.getCuota());
        nuevo.setBonificacion(original.getBonificacion());
        nuevo.setRecargo(original.getRecargo());
        nuevo.setAFavor(original.getAFavor());
        nuevo.setValorBase(original.getValorBase());
        nuevo.setTipo(original.getTipo());
        nuevo.setImporteInicial(nuevoAbono);
        nuevo.setImportePendiente(0.0);
        nuevo.setaCobrar(nuevoAbono);
        nuevo.setCobrado(true);
        return nuevo;
    }

    // **********************************************************************
    // Método auxiliar para crear un nuevo pago a partir de la actualización de cobranza
    // Se asocia el nuevo pago al alumno, inscripción, etc., y se le asigna el monto total abonado.
    // **********************************************************************
    private Pago crearNuevoPago(PagoRegistroRequest request, Pago historico,
                                List<DetallePago> detallesNuevoPago, double montoNuevo) {
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setRecargoAplicado(historico.getRecargoAplicado());
        nuevoPago.setBonificacionAplicada(historico.getBonificacionAplicada());
        nuevoPago.setTipoPago(TipoPago.GENERAL);
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setSaldoAFavor(0.0);
        // Se asocian los detalles clonados al nuevo pago.
        detallesNuevoPago.forEach(d -> d.setPago(nuevoPago));
        nuevoPago.setDetallePagos(detallesNuevoPago);
        nuevoPago.setMonto(montoNuevo);
        // Se cierra el nuevo pago (saldo restante 0, pues se refleja solo el abono).
        nuevoPago.setSaldoRestante(0.0);
        return pagoRepositorio.save(nuevoPago);
    }

    // **********************************************************************
    // Método para procesar el primer pago (cuando existe inscripción)
    // **********************************************************************
    @Transactional
    public Pago processFirstPayment(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno) {
        log.info("[processFirstPayment] Iniciando primer pago para inscripción id={} y alumno id={}",
                inscripcion.getId(), alumno.getId());
        // 1. Crear la base del pago.
        Pago pago = crearPagoBase(request, alumno, inscripcion);
        log.debug("[processFirstPayment] Pago base creado: {}", pago);

        // 2. Convertir cada detalle del request a entidad y calcular importes.
        List<DetallePago> detalles = request.detallePagos().stream()
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);
                    det.setId(null);
                    det.setPago(pago);
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(0.0);
                        log.debug("[processFirstPayment] Detalle sin aCobrar; asignado 0.0: {}", det);
                    }
                    double base = det.getValorBase();
                    double descuento = detallePagoServicio.calcularDescuento(det, base);
                    double recargo = (det.getRecargo() != null)
                            ? detallePagoServicio.obtenerValorRecargo(det, base)
                            : 0.0;
                    double importeInicial = base - descuento + recargo;
                    log.info("[processFirstPayment] Detalle '{}' | Base={}, Descuento={}, Recargo={}, ImporteInicial={}",
                            det.getConcepto(), base, descuento, recargo, importeInicial);
                    det.setImporteInicial(importeInicial);
                    double importePendiente = importeInicial - det.getaCobrar();
                    if (importePendiente < 0) {
                        log.warn("[processFirstPayment] Importe pendiente negativo para '{}'. Se asigna 0.", det.getConcepto());
                        importePendiente = 0;
                    }
                    det.setImportePendiente(importePendiente);
                    return det;
                })
                .filter(det -> det.getImportePendiente() != null && det.getImportePendiente() >= 0)
                .collect(Collectors.toList());
        pago.setDetallePagos(detalles);

        // 3. Calcular el monto total del pago (suma de aCobrar de cada detalle).
        double montoTotal = detalles.stream().mapToDouble(det -> det.getaCobrar() != null ? det.getaCobrar() : 0.0).sum();
        pago.setMonto(montoTotal);
        log.debug("[processFirstPayment] Monto total calculado: {}", montoTotal);

        Pago guardado = pagoRepositorio.save(pago);
        log.info("[processFirstPayment] Pago guardado inicialmente: id={}, monto={}", guardado.getId(), guardado.getMonto());
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);
        log.info("[processFirstPayment] Primer pago procesado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
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
                    log.debug("[marcarDetallesCobradosSiImporteEsCero] Detalle id={} con importePendiente={}", detalle.getId(), detalle.getImportePendiente());
                }
            });
        } else {
            // Si no existen detalles asociados al pago, se genera una advertencia.
            log.warn("[marcarDetallesCobradosSiImporteEsCero] Pago id={} no tiene detalles.", pago.getId());
        }
        // Se guarda el estado actualizado del pago en la base de datos.
        pagoRepositorio.save(pago);
    }

    private static void verificarSaldoRestante(Pago pagoFinal) {
        if (pagoFinal.getSaldoRestante() < 0) {
            log.error("Error: Saldo restante negativo detectado en pago ID={}. Ajustando a 0.", pagoFinal.getId());
            pagoFinal.setSaldoRestante(0.0);
        }
    }

    // 3. Método para actualizar un pago existente aplicando un nuevo abono parcial.
    // Se actualizan los detalles existentes (reduciendo el importe pendiente) y se agregan nuevos detalles (si se envían).
    public Pago processPaymentWithPrevious(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno, Pago pagoExistente) {
        log.info("[processPaymentWithPrevious] Actualizando pago existente id={} para alumno id={}",
                pagoExistente.getId(), alumno.getId());

        // Se actualizan los campos comunes del pago (como fechas, método de pago, etc.).
        actualizarCamposPago(pagoExistente, request, alumno, inscripcion);

        // Se obtiene el monto del nuevo abono que se aplicará.
        double nuevoAbono = request.monto();
        if (nuevoAbono < 0) {
            throw new IllegalArgumentException("El monto del abono no puede ser negativo.");
        }

        // Se recorre cada detalle existente para descontar el nuevo abono del importe pendiente.
        pagoExistente.getDetallePagos().forEach(det -> {
            double pendienteActual = det.getImportePendiente();
            // Se calcula el nuevo pendiente restando el nuevo abono.
            double nuevoPendiente = pendienteActual - nuevoAbono;
            if (nuevoPendiente < 0) {
                nuevoPendiente = 0; // Evita que el pendiente sea negativo.
            }
            log.info("[processPaymentWithPrevious] Detalle id={} | Pendiente actual: {} | Nuevo abono: {} | Nuevo pendiente: {}",
                    det.getId(), pendienteActual, nuevoAbono, nuevoPendiente);
            det.setImportePendiente(nuevoPendiente);
            // Se marca el detalle como cobrado si el nuevo pendiente es 0.
            if (nuevoPendiente == 0) {
                det.setCobrado(true);
                log.info("[processPaymentWithPrevious] Detalle id={} marcado como COBRADO.", det.getId());
            } else {
                det.setCobrado(false);
                log.info("[processPaymentWithPrevious] Detalle id={} NO se marca como COBRADO.", det.getId());
            }
        });

        // Se procesan los nuevos detalles que vienen en el request y que no tienen ID (nuevos registros).
        List<DetallePago> nuevosDetalles = request.detallePagos().stream()
                .filter(dto -> dto.id() == null)
                .map(dto -> {
                    // Se convierte el DTO a entidad.
                    DetallePago nuevo = detallePagoMapper.toEntity(dto);
                    nuevo.setPago(pagoExistente);
                    // Se calcula el importe inicial usando el valor base.
                    double importeInicial = nuevo.getValorBase();
                    // Si existe un recargo, se suma al importe inicial.
                    if (nuevo.getRecargo() != null) {
                        importeInicial += detallePagoServicio.obtenerValorRecargo(nuevo, nuevo.getValorBase());
                    }
                    // Se calcula el descuento y se resta del importe inicial, si corresponde.
                    double descuento = detallePagoServicio.calcularDescuento(nuevo, nuevo.getValorBase());
                    if (descuento > 0) {
                        importeInicial -= descuento;
                    }
                    nuevo.setImporteInicial(importeInicial);
                    // Se aplica el abono enviado (si existe) para determinar el importe pendiente.
                    double abono = (dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : 0;
                    double pendiente = Math.max(importeInicial - abono, 0);
                    nuevo.setImportePendiente(pendiente);
                    // Se marca el detalle como cobrado si el importe pendiente es 0.
                    nuevo.setCobrado(pendiente == 0);
                    return nuevo;
                }).collect(Collectors.toList());
        // Si existen nuevos detalles, se agregan al pago existente.
        if (!nuevosDetalles.isEmpty()) {
            log.info("[processPaymentWithPrevious] Nuevos detalles a agregar: {}", nuevosDetalles);
            pagoExistente.getDetallePagos().addAll(nuevosDetalles);
        }

        // Se actualizan los importes totales del pago (monto, montoPagado, saldoRestante, etc.).
        actualizarImportesPago(pagoExistente);
        // Se procesa cualquier logica adicional específica del pago.
        procesarPagosEspecificos(pagoExistente);
        // Se guarda el pago actualizado en la base de datos.
        Pago guardado = pagoRepositorio.save(pagoExistente);
        log.info("[processPaymentWithPrevious] Pago actualizado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
        return guardado;
    }

    // Método para crear un pago base a partir de los datos del request.
    // Este método inicializa los campos principales del pago (fecha, monto, alumno, inscripcion, etc.)
    // sin incluir aún los detalles del pago, los cuales se procesarán en métodos posteriores.
    private Pago crearPagoBase(PagoRegistroRequest request, Alumno alumno, Inscripcion inscripcion) {
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setAlumno(alumno);
        pago.setInscripcion(inscripcion);
        pago.setSaldoRestante(request.monto());
        pago.setSaldoAFavor(0.0);
        pago.setEstadoPago(EstadoPago.ACTIVO);  // Se asigna como activo
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
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
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        pago.setMonto(request.monto());
        pago.setSaldoAFavor(0.0);
    }

    // Obtiene nuevos detalles a partir del request (los que no tengan ID)
    private List<DetallePago> obtenerDetallesNuevos(PagoRegistroRequest request, Pago pago) {
        List<DetallePago> nuevos = request.detallePagos().stream()
                .filter(detDTO -> detDTO.id() == null)
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);

                    // Si aCobrar no se envía, se asigna 0.0
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(0.0);
                        log.debug("[obtenerDetallesNuevos] No se recibio aCobrar; asignando 0.0 para detalle con valorBase={}", det.getValorBase());
                    }

                    // Asignar el pago al detalle
                    det.setPago(pago);

                    // Calcular importeInicial y, a partir de este, el importePendiente
                    // Aquí se asume que importeInicial es igual a valorBase; si existe logica adicional (descuentos/recargos), aplicarla aquí.
                    if (det.getValorBase() != null) {
                        double importeInicial = det.getValorBase(); // Puedes ajustar este cálculo según tus reglas de negocio.
                        det.setImporteInicial(importeInicial);
                        // El importe pendiente se calcula restando lo que se abona (aCobrar) al importe inicial.
                        det.setImportePendiente(importeInicial - det.getaCobrar());
                    }
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
        log.debug("[obtenerDetallesNuevos] Nuevos detalles obtenidos: {}", nuevos);
        return nuevos;
    }

    // **********************************************************************
    // Método para actualizar los importes totales del pago (saldo, montoPagado, etc.)
    // **********************************************************************
    public void actualizarImportesPago(Pago pago) {
        log.info("[actualizarImportesPago] Recalculando importes para pago id={}", pago.getId());

        // Detectar si es un pago de resumen (p. ej., si tiene un único detalle con código "RESUMEN")
        boolean esPagoResumen = false;
        if (pago.getDetallePagos() != null && pago.getDetallePagos().size() == 1) {
            DetallePago detalle = pago.getDetallePagos().get(0);
            if ("RESUMEN".equalsIgnoreCase(detalle.getCodigoConcepto())) {
                esPagoResumen = true;
            }
        }

        if (!esPagoResumen) {
            // Para pagos regulares: recalcular cada detalle y luego el saldo y monto pagado.
            pago.getDetallePagos().forEach(detallePago -> {
                log.debug("[actualizarImportesPago] Iniciando cálculo para detalle id={} (concepto: {})",
                        detallePago.getId(), detallePago.getConcepto());
                detallePagoServicio.calcularImporte(detallePago);
                log.debug("[actualizarImportesPago] Detalle id={} recalculado: ImporteInicial={}, ImportePendiente={}",
                        detallePago.getId(), detallePago.getImporteInicial(), detallePago.getImportePendiente());
            });

            // Calcular saldoRestante sumando el importe pendiente de cada detalle.
            double totalPendiente = pago.getDetallePagos().stream()
                    .mapToDouble(DetallePago::getImportePendiente)
                    .sum();
            pago.setSaldoRestante(totalPendiente);
            log.debug("[actualizarImportesPago] SaldoRestante recalculado: {}", totalPendiente);

            // Calcular montoPagado como (total de importes iniciales - total pendiente).
            double totalInicial = pago.getDetallePagos().stream()
                    .mapToDouble(DetallePago::getImporteInicial)
                    .sum();
            pago.setMontoPagado(totalInicial - totalPendiente);
            log.debug("[actualizarImportesPago] MontoPagado actualizado: {}", pago.getMontoPagado());
        } else {
            // Para pagos de resumen: conservar el saldoRestante ya asignado
            double totalInicial = pago.getDetallePagos().stream()
                    .mapToDouble(DetallePago::getImporteInicial)
                    .sum();
            // Solo se actualiza el montoPagado sin tocar el saldoRestante
            pago.setMontoPagado(totalInicial - pago.getSaldoRestante());
            log.debug("[actualizarImportesPago] Pago RESUMEN: MontoPagado actualizado: {}", pago.getMontoPagado());
        }

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[actualizarImportesPago] Pago id={} actualizado. MontoPagado={}, SaldoRestante={}",
                pago.getId(), pago.getMontoPagado(), pago.getSaldoRestante());
    }

    // **********************************************************************
    // Método para procesar pagos específicos según el concepto del detalle
    // (MATRICULA, MENSUALIDAD, GENERAL, etc.)
    // **********************************************************************
    public void procesarPagosEspecificos(Pago pago) {
        log.info("[procesarPagosEspecificos] Iniciando procesamiento de detalles para pago id={}", pago.getId());
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                // Si el detalle no tiene tipo definido, se intenta determinarlo a partir del concepto
                if (detalle.getTipo() == null) {
                    if (detalle.getConcepto() != null) {
                        String concepto = detalle.getConcepto().trim().toUpperCase();
                        log.debug("[procesarPagosEspecificos] Concepto normalizado: '{}'", concepto);
                        if (concepto.startsWith("MATRICULA")) {
                            procesarMatricula(pago, detalle);
                        } else if (concepto.contains("CUOTA")) {
                            procesarMensualidad(pago, detalle);
                        } else {
                            procesarDetalleGeneral(pago, detalle);
                        }
                    } else {
                        log.warn("[procesarPagosEspecificos] Detalle id={} sin concepto, se omite", detalle.getId());
                    }
                } else {
                    // Si el tipo ya está definido, se llama al procesamiento correspondiente.
                    switch (detalle.getTipo()) {
                        case MATRICULA:
                            procesarMatricula(pago, detalle);
                            break;
                        case MENSUALIDAD:
                            procesarMensualidad(pago, detalle);
                            break;
                        case GENERAL:
                        default:
                            procesarDetalleGeneral(pago, detalle);
                            break;
                    }
                }
            }
        } else {
            log.warn("[procesarPagosEspecificos] El pago id={} no contiene detalles", pago.getId());
        }
        log.info("[procesarPagosEspecificos] Finalizado procesamiento de detalles para pago id={}", pago.getId());
    }

    // Procesamiento para matrícula (ejemplo sin cambios significativos)
    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matrícula para detalle id={}", detalle.getId());
        if (pago.getAlumno() != null) {
            String[] partes = detalle.getConcepto().split(" ");
            log.debug("[procesarMatricula] Partes del concepto: {}", Arrays.toString(partes));
            if (partes.length >= 2) {
                try {
                    int anio = Integer.parseInt(partes[1]);
                    log.info("[procesarMatricula] Procesando matrícula para el año {}", anio);
                    MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                    log.debug("[procesarMatricula] Matrícula pendiente encontrada: {}", matResp);
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
                double montoAbonadoAcumulado = pago.getDetallePagos().stream()
                        .filter(d -> d.getConcepto().trim().equalsIgnoreCase(descripcionDetalle))
                        .mapToDouble(DetallePago::getaCobrar)
                        .sum();
                log.info("[procesarMensualidad] Mensualidad id={} ('{}'): monto abonado acumulado={} y total a pagar={}",
                        mensPendiente.id(), descripcionDetalle, montoAbonadoAcumulado, mensPendiente.totalPagar());
                if (montoAbonadoAcumulado >= mensPendiente.totalPagar()) {
                    log.info("[procesarMensualidad] Condicion cumplida: {} >= {}. Marcando mensualidad id={} como PAGADO.",
                            montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.marcarComoPagada(mensPendiente.id(), pago.getFecha());
                } else {
                    log.info("[procesarMensualidad] Abono parcial: {} < {}. Actualizando abono parcial para mensualidad id={}.",
                            montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.actualizarAbonoParcial(mensPendiente.id(), montoAbonadoAcumulado);
                }
            } else {
                log.info("[procesarMensualidad] No se encontro mensualidad pendiente para '{}'. Se creará una nueva.",
                        descripcionDetalle);
                mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), descripcionDetalle, pago.getFecha());
            }
        } else {
            log.info("[procesarMensualidad] Pago GENERAL: se omite procesamiento de mensualidades sin inscripcion.");
        }
    }

    // Procesamiento para detalle general (sin logica adicional)
    private void procesarDetalleGeneral(Pago pago, DetallePago detalle) {
        log.info("[procesarDetalleGeneral] Iniciando procesamiento de detalle general para detalle id={} con concepto '{}'",
                detalle.getId(), detalle.getConcepto());
        log.debug("[procesarDetalleGeneral] Detalle general sin procesamiento adicional. Se registra tal cual.");
        log.info("[procesarDetalleGeneral] Finalizado procesamiento de detalle general para detalle id={}", detalle.getId());
    }

    private Pago obtenerPagoHistorico(Long pagoId) {
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para actualizacion."));
        log.debug("[obtenerPagoHistorico] Pago obtenido: id={}, saldoRestante={}", historico.getId(), historico.getSaldoRestante());
        if (historico.getSaldoRestante() == 0) {
            log.error("[obtenerPagoHistorico] El pago id={} tiene saldoRestante=0, no se puede actualizar como histórico.", historico.getId());
            throw new IllegalArgumentException("El pago ya está saldado y no se puede actualizar como historico.");
        }
        return historico;
    }

    private Map<String, DetallePagoRegistroRequest> mapearNuevosAbonos(PagoRegistroRequest request) {
        return request.detallePagos().stream()
                .collect(Collectors.toMap(dto -> dto.concepto().trim().toLowerCase(), Function.identity()));
    }

    // **********************************************************************
    // Método para validar si el pago histórico es aplicable para ser actualizado.
    // Se verifica que para cada detalle enviado en el request exista un detalle en el pago histórico
    // que tenga el mismo concepto (ignorando mayúsculas y espacios).
    // **********************************************************************
    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null) {
            log.debug("[esPagoHistoricoAplicable] Pago histórico es nulo, retornando false.");
            return false;
        }
        boolean aplicable = request.detallePagos().stream().allMatch(dto -> {
            String conceptoRequest = dto.concepto().trim().toLowerCase();
            boolean existe = ultimoPendiente.getDetallePagos().stream().anyMatch(det -> {
                String conceptoHistorico = det.getConcepto().trim().toLowerCase();
                boolean coincide = conceptoHistorico.equals(conceptoRequest);
                log.debug("[esPagoHistoricoAplicable] Comparando request concepto '{}' con histórico concepto '{}': {}",
                        conceptoRequest, conceptoHistorico, coincide);
                return coincide;
            });
            log.debug("[esPagoHistoricoAplicable] Para el concepto '{}' del request, ¿existe en histórico? {}",
                    conceptoRequest, existe);
            return existe;
        });
        log.debug("[esPagoHistoricoAplicable] Resultado final: {}", aplicable);
        return aplicable;
    }

    // **********************************************************************
    // Método para decidir el flujo de pago según si hay inscripción y si se puede actualizar un histórico
    // **********************************************************************
    public Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        log.info("[crearPagoSegunInscripcion] Procesando pago para inscripción: {}", request.inscripcion());

        // Si no hay inscripción o es -1, se procesa como pago GENERAL.
        if (request.inscripcion().id() == null || request.inscripcion().id() == -1) {
            log.info("[crearPagoSegunInscripcion] No se detectó inscripción válida. Procesando como pago GENERAL.");
            return processGeneralPayment(request);
        }

        // Se obtiene la inscripción y el alumno.
        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcion().id())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Alumno alumno = inscripcion.getAlumno();

        // Se consulta el último pago pendiente del alumno.
        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (ultimoPendiente != null) {
            log.debug("[crearPagoSegunInscripcion] Último pago pendiente: id={}, saldoRestante={}",
                    ultimoPendiente.getId(), ultimoPendiente.getSaldoRestante());
        }

        // Si existe un histórico con saldo > 0 y es aplicable, se delega a actualizarCobranzaHistorica.
        if (ultimoPendiente != null
                && ultimoPendiente.getSaldoRestante() > 0
                && esPagoHistoricoAplicable(ultimoPendiente, request)) {
            log.info("[crearPagoSegunInscripcion] Se detectó pago histórico aplicable; se actualizará.");
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            if (ultimoPendiente != null && ultimoPendiente.getSaldoRestante() == 0) {
                log.info("[crearPagoSegunInscripcion] Pago histórico saldado; se creará un nuevo pago.");
            } else {
                log.info("[crearPagoSegunInscripcion] No se cumple condición de histórico; se procesa un nuevo pago.");
            }
            return processFirstPayment(request, inscripcion, alumno);
        }
    }

    // **********************************************************************
    // Método para obtener el último pago pendiente (activo) de un alumno.
    // Se asume que el repositorio filtra por estado ACTIVO.
    // **********************************************************************
    Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        Optional<Pago> optPago = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO);
        if(optPago.isPresent()){
            Pago pago = optPago.get();
            log.debug("[obtenerUltimoPagoPendienteEntidad] Se encontró pago: id={}, saldoRestante={}", pago.getId(), pago.getSaldoRestante());
            return pago;
        } else {
            log.debug("[obtenerUltimoPagoPendienteEntidad] No se encontró pago pendiente para alumnoId={}", alumnoId);
            return null;
        }
    }
}
