package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * -------------------------------------------------------------------------------------------------
 * 📌 Refactor del Servicio PaymentProcessor
 * Se ha consolidado la lógica de procesamiento de cada DetallePago en un único método:
 * - Se elimina la duplicidad entre procesarDetallePago y calcularImporte, centralizando el flujo en
 * {@code procesarYCalcularDetalle(Pago, DetallePago)}.
 * - Se centraliza el cálculo del abono y la actualización de importes en el método
 * {@code procesarAbono(...)}.
 * - La determinación del tipo de detalle se realiza siempre mediante {@code determinarTipoDetalle(...)}.
 * - Se diferencia claramente entre el caso de pago nuevo (donde se clona el detalle si ya existe en BD)
 * y el de actualización (se carga el detalle persistido y se actualizan sus campos).
 * - Finalmente, se asegura que al finalizar el procesamiento de cada detalle se actualicen los totales
 * del pago y se verifiquen los estados relacionados (por ejemplo, marcar mensualidad o matrícula como
 * pagada, o reducir el stock).
 * -------------------------------------------------------------------------------------------------
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final PaymentCalculationServicio paymentCalculationServicio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;

    // Mappers

    // Servicios auxiliares
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            PaymentCalculationServicio calculationServicio,
                            MatriculaServicio matriculaServicio,
                            MensualidadServicio mensualidadServicio,
                            StockServicio stockServicio,
                            DetallePagoServicio detallePagoServicio,
                            MatriculaMapper matriculaMapper,
                            AlumnoMapper alumnoMapper,
                            PagoMapper pagoMapper,
                            DetallePagoRepositorio detallePagoRepositorio, PaymentCalculationServicio paymentCalculationServicio, DetallePagoMapper detallePagoMapper, DetallePagoMapper detallePagoMapper1) {
        this.pagoRepositorio = pagoRepositorio;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
    }

    // 3. Reatachar asociaciones (manteniendo la idea original)
    void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
            log.info("[reatacharAsociaciones] Alumno asignado: id={} al DetallePago id={}",
                    pago.getAlumno().getId(), detalle.getId());
        }
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getId() != null) {
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
            }
        }
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
            }
        }
        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
            }
        }
    }

    // 7. Recalcular totales: sumar aCobrar e importePendiente de cada detalle activo
    void recalcularTotales(Pago pago) {
        log.info("[recalcularTotales] Iniciando recálculo de totales para Pago id={}", pago.getId());
        BigDecimal montoTotalAbonado = BigDecimal.ZERO;
        BigDecimal saldoTotal = BigDecimal.ZERO;

        for (DetallePago detalle : pago.getDetallePagos()) {
            // Se calcula el abono aplicado como la diferencia entre el importeInicial y el importePendiente.
            double abonoAplicado = detalle.getImporteInicial() - detalle.getImportePendiente();
            log.info("[recalcularTotales] Procesando DetallePago id={} - Abono aplicado: {}, Importe pendiente: {}",
                    detalle.getId(), abonoAplicado, detalle.getImportePendiente());
            montoTotalAbonado = montoTotalAbonado.add(BigDecimal.valueOf(abonoAplicado));
            saldoTotal = saldoTotal.add(BigDecimal.valueOf(detalle.getImportePendiente()));
        }

        pago.setMonto(montoTotalAbonado.doubleValue());
        pago.setMontoPagado(montoTotalAbonado.doubleValue());
        pago.setSaldoRestante(saldoTotal.doubleValue());
        log.info("[recalcularTotales] Totales actualizados para Pago id={}: Monto total abonado={}, Saldo restante={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante());
    }

    /**
     * Método auxiliar para obtener la inscripción a partir del detalle (si corresponde).
     */
    Inscripcion obtenerInscripcion(DetallePago detalle) {
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getInscripcion() != null) {
            return detalle.getMensualidad().getInscripcion();
        }
        return null;
    }

    Pago loadAndUpdatePago(Pago pago) {
        Pago pagoManaged = entityManager.find(Pago.class, pago.getId());
        if (pagoManaged == null) {
            throw new EntityNotFoundException("Pago no encontrado para id: " + pago.getId());
        }
        pagoManaged.setFecha(pago.getFecha());
        pagoManaged.setFechaVencimiento(pago.getFechaVencimiento());
        pagoManaged.setMonto(pago.getMonto());
        pagoManaged.setImporteInicial(pago.getImporteInicial());
        return pagoManaged;
    }

    // Método auxiliar para actualizar los campos modificables de un DetallePago.
    private void actualizarCamposDetalle(DetallePago detallePersistido, DetallePago detalleRecibido) {
        detallePersistido.setDescripcionConcepto(detalleRecibido.getDescripcionConcepto());
        detallePersistido.setCuotaOCantidad(detalleRecibido.getCuotaOCantidad());
        detallePersistido.setValorBase(detalleRecibido.getValorBase());
        detallePersistido.setBonificacion(detalleRecibido.getBonificacion());
        detallePersistido.setRecargo(detalleRecibido.getRecargo());
    }

    @Transactional
    protected DetallePago actualizarDetalle(DetallePago detalleRecibido, Pago pagoManaged, Alumno alumno) {
        if (detalleRecibido.getId() == null) {
            log.error("[actualizarDetalle] El detalle no tiene ID asignado.");
            throw new IllegalStateException("Se esperaba que todos los detalles tuvieran ID asignado");
        }

        DetallePago detallePersistido = entityManager.find(DetallePago.class, detalleRecibido.getId());
        if (detallePersistido == null) {
            log.info("[actualizarDetalle] entityManager.find() devolvió null. Se intenta con JPQL para id={}", detalleRecibido.getId());
            detallePersistido = detallePagoRepositorio.buscarPorIdJPQL(detalleRecibido.getId());
        }
        if (detallePersistido == null) {
            log.error("[actualizarDetalle] No se encontró detalle con id={} en la BD.", detalleRecibido.getId());
            throw new IllegalStateException("El detalle con id=" + detalleRecibido.getId() + " no se encontró en la BD");
        }
        log.info("[actualizarDetalle] Detalle encontrado: id={}, version={}", detallePersistido.getId(), detallePersistido.getVersion());

        // Actualizar los campos modificables (se reutiliza el método auxiliar)
        actualizarCamposDetalle(detallePersistido, detalleRecibido);

        // Reasociar el detalle al pago y al alumno
        detallePersistido.setPago(pagoManaged);
        detallePersistido.setAlumno(alumno);

        // Si el importe pendiente es nulo, se calcula a partir del importeInicial y aCobrar (si existe)
        if (detallePersistido.getImportePendiente() == null) {
            double aCobrar = Optional.ofNullable(detallePersistido.getaCobrar()).orElse(0.0);
            double nuevoPendiente = (detallePersistido.getImporteInicial() != null)
                    ? detallePersistido.getImporteInicial() - aCobrar
                    : 0.0;
            log.info("[actualizarDetalle] Calculando importePendiente para detalle id={}: {} - {} = {}",
                    detallePersistido.getId(), detallePersistido.getImporteInicial(), aCobrar, nuevoPendiente);
            detallePersistido.setImportePendiente(nuevoPendiente);
        }

        // Actualizar los estados relacionados en función de la fecha del pago
        actualizarEstadosRelacionados(detallePersistido, pagoManaged.getFecha());
        log.info("[actualizarDetalle] Detalle id={} actualizado: cobrado={}, tipo={}",
                detallePersistido.getId(), detallePersistido.getCobrado(), detallePersistido.getTipo());

        return detallePersistido;
    }

    /**
     * Actualiza los estados relacionados en el detalle cuando se salda (importePendiente <= 0).
     */
    private void actualizarEstadosRelacionados(DetallePago detalle, LocalDate fechaPago) {
        if (detalle.getImportePendiente() != null && detalle.getImportePendiente() <= 0.0 && !detalle.getCobrado()) {
            detalle.setCobrado(true);
            switch (detalle.getTipo()) {
                case MENSUALIDAD -> {
                    if (detalle.getMensualidad() != null) {
                        mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), fechaPago);
                    }
                }
                case MATRICULA -> {
                    if (detalle.getMatricula() != null) {
                        matriculaServicio.actualizarEstadoMatricula(
                                detalle.getMatricula().getId(),
                                new MatriculaRegistroRequest(detalle.getAlumno().getId(),
                                        detalle.getMatricula().getAnio(),
                                        true,
                                        fechaPago));
                    }
                }
                case STOCK -> {
                    if (detalle.getStock() != null) {
                        stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
                    }
                }
                default -> {
                    // Otros conceptos no requieren actualización extra
                }
            }
        }
    }

    private int extraerAnioDeDescripcion(String desc) {
        try {
            String[] partes = desc.split(" ");
            return Integer.parseInt(partes[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo extraer el año de la matrícula en '" + desc + "'");
        }
    }

    // 1. Obtener el último pago pendiente (se mantiene similar, verificando saldo > 0)
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

    /**
     * Verifica que el saldo restante no sea negativo. Si es negativo, lo ajusta a 0.
     */
    Pago verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.",
                    pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}",
                    pago.getId(), pago.getSaldoRestante());
        }
        return pago;
    }

    // 2. Determinar si es aplicable el pago histórico, estandarizando la generación de claves
    boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        log.info("[esPagoHistoricoAplicable] Iniciando verificación para pago histórico.");
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] No se puede aplicar pago histórico, detallePagos es nulo.");
            return false;
        }

        // Generación de claves para el último pago
        Set<String> clavesHistoricas = ultimoPendiente.getDetallePagos().stream()
                .peek(det -> log.info("[esPagoHistoricoAplicable] Detalle histórico id={} - Concepto: {}, SubConcepto: {}",
                        det.getId(), det.getConcepto(), det.getSubConcepto()))
                .map(det ->
                        (det.getConcepto() != null && det.getConcepto().getId() != null ? det.getConcepto().getId().toString() : "")
                                + "_" +
                                (det.getSubConcepto() != null && det.getSubConcepto().getId() != null ? det.getSubConcepto().getId().toString() : ""))
                .filter(clave -> !clave.equals("_"))
                .peek(clave -> log.info("[esPagoHistoricoAplicable] Clave generada para detalle histórico: '{}'", clave))
                .collect(Collectors.toSet());

        // Generación de claves para el request
        Set<String> clavesRequest = request.detallePagos().stream()
                .peek(dto -> log.info("[esPagoHistoricoAplicable] Request detalle - ConceptoId: {}, SubConceptoId: {}",
                        dto.conceptoId(), dto.subConceptoId()))
                .map(dto ->
                        (dto.conceptoId() != null ? dto.conceptoId().toString() : "")
                                + "_" +
                                (dto.subConceptoId() != null ? dto.subConceptoId().toString() : ""))
                .filter(clave -> !clave.equals("_"))
                .peek(clave -> log.info("[esPagoHistoricoAplicable] Clave generada para request detalle: '{}'", clave))
                .collect(Collectors.toSet());

        boolean aplicable = !clavesRequest.isEmpty() && clavesHistoricas.containsAll(clavesRequest);
        log.info("[esPagoHistoricoAplicable] clavesHistoricas={}, clavesRequest={}, aplicable={}",
                clavesHistoricas, clavesRequest, aplicable);
        return aplicable;
    }

    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        log.info("[actualizarCobranzaHistorica] Iniciando actualización para pago histórico id={}", pagoId);
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago histórico no encontrado con ID=" + pagoId));
        log.info("[actualizarCobranzaHistorica] Pago histórico obtenido: id={}, saldoRestante={}",
                historico.getId(), historico.getSaldoRestante());

        // Mapeo de abonos mediante clave compuesta (conceptoId_subConceptoId)
        Map<String, Double> mapaAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(
                        dto -> dto.conceptoId() + "_" + dto.subConceptoId(),
                        DetallePagoRegistroRequest::aCobrar,
                        Double::sum
                ));
        log.info("[actualizarCobranzaHistorica] Mapa de abonos: {}", mapaAbonos);

        // Aplicar abonos a cada detalle y reprocesar de forma unificada
        historico.getDetallePagos().forEach(detalle -> {
            String key = (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "null")
                    + "_" + (detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : "null");
            Double abono = mapaAbonos.getOrDefault(key, 0.0);
            log.info("[actualizarCobranzaHistorica] Para detalle id={} (clave={}), abono asignado={}",
                    detalle.getId(), key, abono);

            // Utilizar el método unificado de abono
            paymentCalculationServicio.procesarAbono(detalle, abono, null);
            // Reprocesar el detalle tras aplicar el abono
            Inscripcion inscripcion = (detalle.getMensualidad() != null)
                    ? detalle.getMensualidad().getInscripcion()
                    : null;
            paymentCalculationServicio.procesarYCalcularDetalle(historico, detalle, inscripcion);
        });

        // Marcar el pago histórico como saldado y persistirlo
        historico.setEstadoPago(EstadoPago.HISTORICO);
        historico.setSaldoRestante(0.0);
        pagoRepositorio.save(historico);
        log.info("[actualizarCobranzaHistorica] Pago histórico id={} marcado como HISTÓRICO y guardado.", historico.getId());

        // Crear un nuevo pago a partir del histórico (clonando solo los detalles pendientes)
        Pago nuevoPago = crearNuevoPagoDesdeHistorico(historico, request);
        pagoRepositorio.save(nuevoPago);
        log.info("[actualizarCobranzaHistorica] Nuevo pago creado a partir del histórico: id={}, monto={}, saldoRestante={}",
                nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());
        return nuevoPago;
    }

    private Pago crearNuevoPagoDesdeHistorico(Pago historico, PagoRegistroRequest request) {
        log.info("[crearNuevoPagoDesdeHistorico] Creando nuevo pago a partir del histórico id={}", historico.getId());
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setObservaciones(historico.getObservaciones());
        log.info("[crearNuevoPagoDesdeHistorico] Datos básicos asignados: fecha={}, fechaVencimiento={}, alumno id={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getAlumno().getId());

        // Extraer los detalles pendientes (con saldo > 0) del pago histórico
        List<DetallePago> pendientes = historico.getDetallePagos().stream()
                .filter(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0) > 0)
                .peek(det -> {
                    // Desasociar el detalle del pago histórico
                    det.setPago(null);
                    log.info("[crearNuevoPagoDesdeHistorico] Desasociando detalle id={} del pago histórico.", det.getId());
                })
                .collect(Collectors.toList());

        // Reasociar cada DetallePago al nuevo pago y reprocesarlo
        pendientes.forEach(det -> {
            det.setPago(nuevoPago);
            log.info("[crearNuevoPagoDesdeHistorico] Reasociando detalle id={} al nuevo pago.", det.getId());
            Inscripcion inscripcion = (det.getMensualidad() != null)
                    ? det.getMensualidad().getInscripcion()
                    : null;
            paymentCalculationServicio.procesarYCalcularDetalle(nuevoPago, det, inscripcion);
        });

        nuevoPago.setDetallePagos(pendientes);
        paymentCalculationServicio.actualizarImportesTotalesPago(nuevoPago);
        log.info("[crearNuevoPagoDesdeHistorico] Nuevo pago con detalles pendientes asignados. Totales actualizados: monto={}, saldoRestante={}",
                nuevoPago.getMonto(), nuevoPago.getSaldoRestante());
        return nuevoPago;
    }

    private boolean existeStockConNombre(String conceptoNorm) {
        // Se verifica si la descripción corresponde a un stock o producto.
        return conceptoNorm.contains("STOCK") || conceptoNorm.contains("PRODUCTO");
    }

    public boolean existeDetalleDuplicado(DetallePago detalle, Long alumnoId) {
        // Consulta para buscar detalles con:
        // - mismo alumno (a través de su pago)
        // - misma descripción normalizada (se asume que ya se normalizó)
        // - mismo tipo de detalle
        // - que no estén marcados como cobrado (es decir, con importePendiente > 0)
        String jpql = "SELECT COUNT(dp) FROM DetallePago dp " +
                "WHERE dp.pago.alumno.id = :alumnoId " +
                "AND dp.descripcionConcepto = :descripcion " +
                "AND dp.tipo = :tipo " +
                "AND dp.cobrado = false";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("alumnoId", alumnoId)
                .setParameter("descripcion", detalle.getDescripcionConcepto())
                .setParameter("tipo", detalle.getTipo())
                .getSingleResult();

        return count > 0;
    }

    @Transactional
    public Pago actualizarPagoHistoricoConAbonos(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago histórico id={} con abonos", pagoHistorico.getId());
        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            // Buscar el detalle correspondiente en el pago histórico
            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> Objects.equals(d.getDescripcionConcepto().toUpperCase().trim(),
                            detalleReq.descripcionConcepto().toUpperCase().trim()))
                    .findFirst();
            if (detalleOpt.isPresent()) {
                DetallePago detalle = detalleOpt.get();
                log.info("[actualizarPagoHistoricoConAbonos] Detalle encontrado para clave '{}': id={}",
                        detalleReq.descripcionConcepto(), detalle.getId());
                // Aplicar el abono sin eliminar el detalle, incluso si queda saldado
                paymentCalculationServicio.procesarAbono(detalle, detalleReq.aCobrar(), detalle.getImporteInicial());
            } else {
                log.warn("[actualizarPagoHistoricoConAbonos] No se encontró detalle para clave '{}'", detalleReq.descripcionConcepto());
            }
        }
        // Recalcular totales tomando en cuenta TODOS los detalles, incluso los saldados
        recalcularTotales(pagoHistorico);
        // Si el saldo restante es 0, marcar el pago histórico como HISTÓRICO
        if (pagoHistorico.getSaldoRestante() <= 0.0) {
            pagoHistorico.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[actualizarPagoHistoricoConAbonos] Pago histórico id={} marcado como HISTÓRICO", pagoHistorico.getId());
        }
        pagoHistorico = pagoRepositorio.save(pagoHistorico);
        log.info("[actualizarPagoHistoricoConAbonos] Pago histórico id={} actualizado. Totales: monto={}, saldoRestante={}",
                pagoHistorico.getId(), pagoHistorico.getMonto(), pagoHistorico.getSaldoRestante());
        return pagoHistorico;
    }

    @Transactional
    public Pago clonarDetallesConPendiente(Pago pagoHistorico) {
        log.info("[clonarDetallesConPendiente] Iniciando clonación de detalles pendientes del pago histórico id={}", pagoHistorico.getId());
        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        nuevoPago.setFecha(LocalDate.now());
        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        nuevoPago.setDetallePagos(new ArrayList<>());

        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            if (detalle.getImportePendiente() > 0.0) {
                DetallePago nuevoDetalle = new DetallePago();
                nuevoDetalle.setDescripcionConcepto(detalle.getDescripcionConcepto());
                nuevoDetalle.setValorBase(detalle.getValorBase());
                nuevoDetalle.setImporteInicial(detalle.getImporteInicial());
                nuevoDetalle.setaCobrar(detalle.getaCobrar());
                nuevoDetalle.setImportePendiente(detalle.getImportePendiente());
                nuevoDetalle.setTipo(detalle.getTipo());
                // <== Aquí asignamos la asociación de forma correcta:
                nuevoDetalle.setPago(nuevoPago);
                nuevoPago.getDetallePagos().add(nuevoDetalle);
                nuevoPago.addDetallePago(nuevoDetalle);

                log.info("[clonarDetallesConPendiente] Detalle clonado: id antiguo={}, importePendiente={}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        recalcularTotales(nuevoPago);
        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] Nuevo pago creado con id={} y {} detalles pendientes",
                nuevoPago.getId(), nuevoPago.getDetallePagos().size());
        return nuevoPago;
    }

}
