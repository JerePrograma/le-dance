package ledance.servicios.pago;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    @PersistenceContext
    private EntityManager entityManager;

    private final PagoRepositorio pagoRepositorio;
    private final PaymentCalculationServicio calculationServicio;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final MatriculaRepositorio matriculaRepositorio;
    private final MatriculaMapper matriculaMapper;
    private final AlumnoMapper alumnoMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final PagoMapper pagoMapper;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            PaymentCalculationServicio calculationServicio,
                            MatriculaServicio matriculaServicio,
                            MensualidadServicio mensualidadServicio,
                            StockServicio stockServicio, DetallePagoServicio detallePagoServicio, MatriculaRepositorio matriculaRepositorio, MatriculaMapper matriculaMapper, AlumnoMapper alumnoMapper, InscripcionRepositorio inscripcionRepositorio, PagoMapper pagoMapper) {
        this.pagoRepositorio = pagoRepositorio;
        this.calculationServicio = calculationServicio;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.matriculaRepositorio = matriculaRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.alumnoMapper = alumnoMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.pagoMapper = pagoMapper;
    }

    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago histórico no encontrado"));

        Map<String, Double> mapaAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(
                        dto -> dto.conceptoId() + "_" + dto.subConceptoId(),
                        DetallePagoRegistroRequest::aCobrar,
                        Double::sum // Fusiona sumando los valores
                ));

        historico.getDetallePagos().forEach(detalle -> {
            String key = (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "null")
                    + "_" + (detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : "null");
            Double abono = mapaAbonos.getOrDefault(key, 0.0);
            calculationServicio.aplicarAbono(detalle, abono);
            actualizarEstadosRelacionados(detalle, historico.getFecha());
        });

        historico.setEstadoPago(EstadoPago.HISTORICO);
        historico.setSaldoRestante(0.0);
        pagoRepositorio.save(historico);

        return crearNuevoPagoDesdeHistorico(historico, request);
    }

    public void actualizarImportesPago(Pago pago) {
        double totalAcobrar = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0.0)
                .sum();
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0)
                .sum();

        pago.setMonto(totalAcobrar);
        pago.setSaldoRestante(totalPendiente);
        pago.setMontoPagado(totalPendiente - totalAcobrar);
        verificarSaldoRestante(pago);
    }

    private void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            pago.setSaldoRestante(0.0);
        }
        if (pago.getSaldoRestante() == 0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        }
    }

    private void actualizarEstadosRelacionados(DetallePago detalle, LocalDate fecha) {
        if (detalle.getImportePendiente() == 0) {
            detalle.setCobrado(true);
            switch (detalle.getTipo()) {
                case MENSUALIDAD -> mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), fecha);
                case MATRICULA -> matriculaServicio.actualizarEstadoMatricula(
                        detalle.getMatricula().getId(),
                        new MatriculaRegistroRequest(detalle.getAlumno().getId(), detalle.getMatricula().getAnio(), true, fecha)
                );
                default -> {
                }
            }
        }
    }

    private Pago crearNuevoPagoDesdeHistorico(Pago historico, PagoRegistroRequest request) {
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setTipoPago(TipoPago.RESUMEN);
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setSaldoAFavor(0.0);

        // Crear detalles a partir del histórico, clonando sólo los pendientes
        List<DetallePago> detallesPendientes = historico.getDetallePagos().stream()
                .filter(det -> det.getImportePendiente() > 0)
                .map(det -> {
                    DetallePago nuevoDetalle = det.clonarConPendiente(nuevoPago);
                    detallePagoServicio.calcularImporte(nuevoDetalle);
                    return nuevoDetalle;
                })
                .collect(Collectors.toList());

        nuevoPago.setDetallePagos(detallesPendientes);
        actualizarImportesPago(nuevoPago);
        return nuevoPago;
    }

    public Pago processFirstPayment(Pago pago) {
        log.info("[processFirstPayment] Iniciando procesamiento del primer pago id={}", pago.getId());

        List<DetallePago> detalles = pago.getDetallePagos();
        if (detalles == null || detalles.isEmpty()) {
            pago.setMontoPagado(0.0);
            pago.setSaldoRestante(0.0);
            return pagoRepositorio.save(pago);
        }

        List<DetallePago> nuevosDetalles = new ArrayList<>();
        for (DetallePago detalle : detalles) {
            if (detalle.getId() != null) {
                // Clonamos el detalle proveniente de un pago anterior
                DetallePago clon = detalle.clonarConPendiente(pago);
                // Procesamos el clon para que se recalculen los importes internos
                procesarDetallePago(pago, clon);
                log.info("[processFirstPayment] Clonado y procesado detalle con id antiguo={} para el nuevo pago id={}",
                        detalle.getId(), pago.getId());
                nuevosDetalles.add(clon);
            } else {
                // Procesamos el detalle nuevo directamente
                procesarDetallePago(pago, detalle);
                nuevosDetalles.add(detalle);
            }
        }
        pago.setDetallePagos(nuevosDetalles);

        // Actualizamos los importes totales del pago, ya que cada detalle tiene sus importes correctos
        actualizarImportesTotalesPago(pago);
        verificarSaldoRestante(pago);

        return pagoRepositorio.save(pago);
    }

    public void actualizarImportesTotalesPago(Pago pago) {
        log.info("[actualizarImportesTotalesPago] Actualizando importes totales del pago id={}", pago.getId());

        double totalACobrar = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0.0)
                .sum();
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0)
                .sum();

        pago.setMonto(totalACobrar);
        pago.setSaldoRestante(totalPendiente);

        // Asigna el monto base (ajusta esta lógica según tu negocio)
        pago.setMontoBasePago(totalACobrar);

        if (totalPendiente == 0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[actualizarImportesTotalesPago] Pago id={} marcado como HISTÓRICO (saldado)", pago.getId());
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
            log.info("[actualizarImportesTotalesPago] Pago id={} permanece ACTIVO, saldoRestante={}", pago.getId(), totalPendiente);
        }
    }

    @Transactional
    public void procesarDetallePago(Pago pago, DetallePago detalle) {
        if(detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
        }
        String conceptoDesc = detalle.getDescripcionConcepto().trim().toUpperCase();
        log.info("[procesarDetallePago] Procesando detalle id={}, concepto='{}'", detalle.getId(), conceptoDesc);

        if (conceptoDescCorrespondeAStock(conceptoDesc)) {
            detalle.setTipo(TipoDetallePago.STOCK);
            procesarStock(detalle);
        } else if (conceptoDescCorrespondeAMatricula(conceptoDesc)) {
            detalle.setTipo(TipoDetallePago.MATRICULA);
            procesarMatricula(pago, detalle);
        } else if (conceptoDescCorrespondeAMensualidad(conceptoDesc)) {
            detalle.setTipo(TipoDetallePago.MENSUALIDAD);
            procesarMensualidad(pago, detalle);
        } else {
            detalle.setTipo(TipoDetallePago.CONCEPTO);
            procesarConcepto(detalle);
        }

        if (detalle.getImportePendiente() <= 0.0 && !detalle.getCobrado()) {
            detalle.setCobrado(true);
        }

        log.info("[procesarDetallePago] Finalizado procesamiento del detalle id={}, tipo asignado={}", detalle.getId(), detalle.getTipo());
    }

    private boolean conceptoDescCorrespondeAMatricula(String descripcion) {
        return descripcion.startsWith("MATRICULA");
    }

    private boolean conceptoDescCorrespondeAMensualidad(String descripcion) {
        return descripcion.contains("CUOTA") ||
                descripcion.contains("CLASE SUELTA") ||
                descripcion.contains("CLASE DE PRUEBA");
    }

    private void procesarConcepto(DetallePago detalle) {
        double importePendiente = detalle.getMontoOriginal() - detalle.getaCobrar();
        detalle.setImporteInicial(detalle.getMontoOriginal());
        detalle.setImportePendiente(Math.max(importePendiente, 0.0));
    }

    private boolean conceptoDescCorrespondeAStock(String descripcion) {
        return stockServicio.obtenerStockPorNombre(descripcion);
    }

    // Método para procesar stock
    private void procesarStock(DetallePago detalle) {
        log.info("[procesarStock] Procesando detalle id={} para Stock", detalle.getId());

        detalle.setTipo(TipoDetallePago.STOCK);
        detalle.setImporteInicial(detalle.getMontoOriginal());

        double pendiente = detalle.getMontoOriginal() - detalle.getaCobrar();
        detalle.setImportePendiente(importePendienteValido(pendiente));

        int cantidad = parseCantidad(detalle.getCuota());
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);

        log.info("[procesarStock] Stock reducido: {}, cantidad: {}", detalle.getDescripcionConcepto(), cantidad);
    }

    // Método para procesar matrícula
    // Método auxiliar para calcular el importe pendiente (puede ajustarse la lógica según necesidades)
    private double calcularImportePendiente(DetallePago detalle) {
        double pendiente = detalle.getMontoOriginal() - detalle.getaCobrar();
        return importePendienteValido(pendiente);
    }

    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Procesando matrícula para detalle id={}", detalle.getId());

        // Se setean los valores básicos
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setImporteInicial(detalle.getMontoOriginal());
        // Asignamos el importe pendiente usando la lógica definida
        detalle.setImportePendiente(calcularImportePendiente(detalle));

        // Se asume que en la descripción viene el período (por ejemplo, "Matricula 2025")
        String[] partes = detalle.getDescripcionConcepto().split(" ");
        log.info("[procesarMatricula] Partes del concepto: {}", Arrays.toString(partes));

        if (partes.length >= 2) {
            try {
                int anio = Integer.parseInt(partes[1]);
                Alumno alumno = pago.getAlumno();
                if (alumno == null && pago.getInscripcion() != null) {
                    alumno = pago.getInscripcion().getAlumno();
                    pago.setAlumno(alumno);
                }
                if (alumno == null) {
                    throw new EntityNotFoundException("No se encontró alumno asociado al pago");
                }

                MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());

                matriculaServicio.actualizarEstadoMatricula(
                        matriculaResp.id(),
                        new MatriculaRegistroRequest(alumno.getId(), anio, true, pago.getFecha())
                );

                log.info("[procesarMatricula] Matrícula actualizada para alumnoId={} año={}", alumno.getId(), anio);

                Matricula matriculaEntity = matriculaMapper.toEntity(matriculaResp);
                detalle.setMatricula(matriculaEntity);
                detalle.setTipo(TipoDetallePago.MATRICULA);
            } catch (NumberFormatException e) {
                log.error("[procesarMatricula] Error al procesar año de matrícula: {}", partes[1]);
            }
        }
    }

    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        log.info("[procesarMensualidad] Procesando mensualidad para detalle id={}", detalle.getId());

        // Se establece el tipo
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        // Si aplica, se puede calcular el importe pendiente (dependiendo de la lógica de negocio)
        detalle.setImportePendiente(calcularImportePendiente(detalle));

        if (pago.getInscripcion() != null) {
            MensualidadResponse mensualidad = mensualidadServicio.buscarMensualidadPendientePorDescripcion(
                    pago.getInscripcion(), detalle.getDescripcionConcepto());

            if (mensualidad != null) {
                double montoAcumulado = calcularMontoAcumuladoConcepto(pago, detalle);

                if (montoAcumulado >= mensualidad.importeInicial() || detalle.getImportePendiente() == 0.0) {
                    mensualidadServicio.marcarComoPagada(mensualidad.id(), LocalDate.now());
                    log.info("[procesarMensualidad] Mensualidad id={} marcada como PAGADA.", mensualidad.id());
                } else {
                    mensualidadServicio.actualizarAbonoParcial(mensualidad.id(), montoAcumulado);
                    log.info("[procesarMensualidad] Actualizado abono parcial para mensualidad id={}", mensualidad.id());
                }
                detalle.setTipo(TipoDetallePago.MENSUALIDAD);
            } else {
                mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), detalle.getDescripcionConcepto(), LocalDate.now());
                log.info("[procesarMensualidad] Creada mensualidad nueva como PAGADA para concepto '{}'", detalle.getDescripcionConcepto());
            }
        } else {
            log.warn("[procesarMensualidad] Pago sin inscripción asociada");
        }
    }

    // Método auxiliar para calcular acumulado
    private double calcularMontoAcumuladoConcepto(Pago pago, DetallePago detalle) {
        return pago.getDetallePagos().stream()
                .filter(det -> detalle.getDescripcionConcepto().equalsIgnoreCase(detalle.getDescripcionConcepto()))
                .mapToDouble(DetallePago::getaCobrar)
                .sum();
    }

    /**
     * Asegura que el importe pendiente no sea negativo.
     *
     * @param importePendiente importe a validar
     * @return importe válido (no negativo)
     */
    private double importePendienteValido(double importePendiente) {
        return Math.max(importePendiente, 0.0);
    }

    /**
     * Convierte la cantidad desde una cadena (cuota) a entero, con fallback a 1.
     *
     * @param cuota valor en texto que representa la cantidad
     * @return cantidad parseada o 1 si es inválido
     */
    private int parseCantidad(String cuota) {
        try {
            return Integer.parseInt(cuota.trim());
        } catch (NumberFormatException e) {
            log.warn("Error al parsear cantidad desde '{}'. Usando valor predeterminado 1.", cuota);
            return 1;
        }
    }

    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        return pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                alumnoId, EstadoPago.ACTIVO, 0.0
        ).orElse(null);
    }

    @Transactional
    public Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        log.info("[crearPagoSegunInscripcion] Procesando pago para inscripción: {}",
                (request.inscripcion() != null ? request.inscripcion().id() : "N/A"));

        Alumno alumno = alumnoMapper.toEntity(request.alumno());
        Long inscId = 0L;
        Inscripcion inscripcion = null;
        if (request.inscripcion() != null && request.inscripcion().id() != null) {
            inscId = request.inscripcion().id();
            // Si el id es menor o igual a cero, lo tratamos como nulo
            if (inscId <= 0L) {
                inscId = null;
            }
            if (inscId != null) {
                Long finalInscId = inscId;
                inscripcion = inscripcionRepositorio.findById(inscId)
                        .orElseThrow(() -> new EntityNotFoundException("Inscripción no encontrada con id=" + finalInscId));
            }
        }

        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());

        if (ultimoPendiente != null) {
            log.info("[crearPagoSegunInscripcion] Último pago pendiente encontrado id={}, saldoRestante={}",
                    ultimoPendiente.getId(), ultimoPendiente.getSaldoRestante());
        }

        boolean esAplicablePagoHistorico = ultimoPendiente != null &&
                ultimoPendiente.getSaldoRestante() > 0 &&
                esPagoHistoricoAplicable(ultimoPendiente, request);

        if (esAplicablePagoHistorico) {
            log.info("[crearPagoSegunInscripcion] Pago histórico aplicable detectado, actualizando pago id={}", ultimoPendiente.getId());
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            log.info("[crearPagoSegunInscripcion] Procesando nuevo pago para alumno id={}", alumno.getId());
            return processFirstPayment(pagoMapper.toEntity(request));
        }
    }

    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] Último pendiente o sus detalles son nulos, retornando false.");
            return false;
        }

        Set<String> clavesHistoricas = ultimoPendiente.getDetallePagos().stream()
                .map(detalle -> (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "null") + "_"
                        + (detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : "null"))
                .collect(Collectors.toSet());

        Set<String> clavesRequest = request.detallePagos().stream()
                .map(dto -> dto.conceptoId() + "_" + dto.subConceptoId())
                .collect(Collectors.toSet());

        boolean aplicable = clavesHistoricas.containsAll(clavesRequest);

        log.info("[esPagoHistoricoAplicable] clavesHistoricas={}, clavesRequest={}, aplicable={}",
                clavesHistoricas, clavesRequest, aplicable);

        return aplicable;
    }

}
