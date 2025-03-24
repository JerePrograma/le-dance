package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
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

    /**
     * Reatacha las asociaciones de un DetallePago (alumno, mensualidad, matrícula, stock)
     * para garantizar que las entidades estén en estado managed y evitar errores de detached.
     *
     * @param detalle el objeto DetallePago a reatachar.
     * @param pago    el objeto Pago asociado.
     */
    public void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
            log.info("[reatacharAsociaciones] Alumno asignado: ID {} al DetallePago ID {}",
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

    /**
     * Recalcula los totales del pago a partir de los detalles procesados.
     *
     * @param pago el objeto Pago al cual se recalcularán sus totales.
     */
    public void recalcularTotales(Pago pago) {
        log.info("[recalcularTotales] Recalculando totales para Pago ID: {}", pago.getId());
        BigDecimal montoTotalAbonado = BigDecimal.ZERO;
        BigDecimal saldoTotal = BigDecimal.ZERO;

        for (DetallePago detalle : pago.getDetallePagos()) {
            // Se calcula el abono aplicado: diferencia entre importeInicial e importePendiente
            double abonoAplicado = detalle.getImporteInicial() - detalle.getImportePendiente();
            log.info("[recalcularTotales] Procesando DetallePago ID: {} - Abono aplicado: {}, Importe pendiente: {}",
                    detalle.getId(), abonoAplicado, detalle.getImportePendiente());
            montoTotalAbonado = montoTotalAbonado.add(BigDecimal.valueOf(abonoAplicado));
            saldoTotal = saldoTotal.add(BigDecimal.valueOf(detalle.getImportePendiente()));
        }

        pago.setMonto(montoTotalAbonado.doubleValue());
        pago.setMontoPagado(montoTotalAbonado.doubleValue());
        pago.setSaldoRestante(saldoTotal.doubleValue());
        log.info("[recalcularTotales] Totales actualizados para Pago ID: {} - Monto total abonado: {}, Saldo restante: {}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante());
    }

    /**
     * Obtiene la inscripción asociada al detalle, si aplica.
     *
     * @param detalle el objeto DetallePago.
     * @return la Inscripcion asociada o null.
     */
    public Inscripcion obtenerInscripcion(DetallePago detalle) {
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getInscripcion() != null) {
            return detalle.getMensualidad().getInscripcion();
        }
        return null;
    }

    /**
     * Carga el pago existente desde la base de datos y actualiza sus campos básicos.
     *
     * @param pago el objeto Pago recibido.
     * @return el objeto Pago gestionado (managed).
     */
    public Pago loadAndUpdatePago(Pago pago) {
        Pago pagoManaged = entityManager.find(Pago.class, pago.getId());
        if (pagoManaged == null) {
            throw new EntityNotFoundException("Pago no encontrado para ID: " + pago.getId());
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

        // Crear y asignar datos básicos del nuevo pago
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setObservaciones(historico.getObservaciones());
        log.info("[crearNuevoPagoDesdeHistorico] Datos básicos asignados: fecha={}, fechaVencimiento={}, alumno id={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getAlumno().getId());

        // Lista para almacenar únicamente aquellos detalles que sigan con saldo pendiente > 0 tras reprocesarlos.
        List<DetallePago> detallesPendientesFinal = new ArrayList<>();

        // Procesar cada detalle del histórico con saldo > 0
        historico.getDetallePagos().stream()
                .filter(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0) > 0)
                .forEach(detalle -> {
                    // Desasociar el detalle del pago histórico
                    detalle.setPago(null);
                    log.info("[crearNuevoPagoDesdeHistorico] Desasociando detalle id={} del pago histórico.", detalle.getId());

                    // Reasociar al nuevo pago
                    detalle.setPago(nuevoPago);
                    log.info("[crearNuevoPagoDesdeHistorico] Reasociando detalle id={} al nuevo pago.", detalle.getId());

                    // Obtener la inscripción si corresponde (por mensualidad)
                    Inscripcion inscripcion = (detalle.getMensualidad() != null)
                            ? detalle.getMensualidad().getInscripcion()
                            : null;

                    // Procesar y recalcular el detalle (esto actualizará importePendiente, etc.)
                    paymentCalculationServicio.procesarYCalcularDetalle(nuevoPago, detalle, inscripcion);

                    // Si después del procesamiento sigue teniendo saldo pendiente > 0, se agrega al nuevo pago.
                    if (Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0) > 0) {
                        detallesPendientesFinal.add(detalle);
                        log.info("[crearNuevoPagoDesdeHistorico] Se mantiene detalle id={} con importePendiente={}",
                                detalle.getId(), detalle.getImportePendiente());
                    } else {
                        log.info("[crearNuevoPagoDesdeHistorico] Se descarta detalle id={} por importePendiente=0", detalle.getId());
                    }
                });

        // Asignar la lista filtrada al nuevo pago
        nuevoPago.setDetallePagos(detallesPendientesFinal);

        // Actualizar totales del nuevo pago
        paymentCalculationServicio.actualizarImportesTotalesPago(nuevoPago);
        log.info("[crearNuevoPagoDesdeHistorico] Nuevo pago con detalles pendientes asignados. Totales actualizados: monto={}, saldoRestante={}",
                nuevoPago.getMonto(), nuevoPago.getSaldoRestante());

        return nuevoPago;
    }

    private boolean existeStockConNombre(String conceptoNorm) {
        // Se verifica si la descripción corresponde a un stock o producto.
        return conceptoNorm.contains("STOCK") || conceptoNorm.contains("PRODUCTO");
    }

    @Transactional
    public Pago actualizarPagoHistoricoConAbonos(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago id={} con abonos", pagoHistorico.getId());

        // Procesar cada detalle recibido en el request
        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto() != null &&
                            d.getDescripcionConcepto().trim().equalsIgnoreCase(detalleReq.descripcionConcepto().trim()))
                    .findFirst();
            if (detalleOpt.isPresent()) {
                DetallePago detalle = detalleOpt.get();
                log.info("[actualizarPagoHistoricoConAbonos] Procesando detalle id={} para '{}'", detalle.getId(), detalleReq.descripcionConcepto());
                paymentCalculationServicio.procesarAbono(detalle, detalleReq.aCobrar(), detalle.getImporteInicial());
            } else {
                log.warn("[actualizarPagoHistoricoConAbonos] No se encontró detalle para '{}'", detalleReq.descripcionConcepto());
            }
        }

        // Recalcular totales tomando en cuenta TODOS los detalles
        recalcularTotales(pagoHistorico);

        // Se persiste el pago actualizado sin marcarlo como HISTÓRICO (esto se hará en el flujo de abono parcial)
        pagoHistorico = pagoRepositorio.save(pagoHistorico);
        log.info("[actualizarPagoHistoricoConAbonos] Pago id={} actualizado. Totales: monto={}, saldoRestante={}",
                pagoHistorico.getId(), pagoHistorico.getMonto(), pagoHistorico.getSaldoRestante());
        return pagoHistorico;
    }

    @Transactional
    public Pago clonarDetallesConPendiente(Pago pagoHistorico) {
        log.info("[clonarDetallesConPendiente] Iniciando clonación de detalles pendientes del pago histórico id={}", pagoHistorico.getId());

        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        nuevoPago.setFecha(LocalDate.now());
        // Se puede optar por ajustar la fecha de vencimiento si la lógica lo requiere.
        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        nuevoPago.setDetallePagos(new ArrayList<>());

        // Iterar sobre cada detalle del pago histórico
        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            // Verificamos que el detalle tenga saldo pendiente (mayor a 0)
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0.0) {
                // Clonar el detalle usando un método helper (que debe copiar los campos necesarios)
                DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);
                nuevoPago.getDetallePagos().add(nuevoDetalle);
                log.info("[clonarDetallesConPendiente] Detalle clonado: id antiguo={}, importePendiente={}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        // Recalcular totales para el nuevo pago antes de persistir
        recalcularTotales(nuevoPago);
        nuevoPago.setImporteInicial(calcularImporteInicialDesdeDetalles(nuevoPago.getDetallePagos()));
        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] Nuevo pago creado con id={} y {} detalles pendientes",
                nuevoPago.getId(), nuevoPago.getDetallePagos().size());

        return nuevoPago;
    }

    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo") Double calcularImporteInicialDesdeDetalles(List<DetallePago> detallePagos) {
        if (detallePagos == null || detallePagos.isEmpty()) {
            return 0.0;
        }

        double total = detallePagos.stream()
                .filter(Objects::nonNull)
                .mapToDouble(detalle -> Optional.ofNullable(detalle.getImporteInicial()).orElse(0.0))
                .sum();

        return Math.max(0.0, total); // Asegura que no sea negativo
    }

    /**
     * Método auxiliar para clonar un DetallePago, copiando todos los campos excepto id y version,
     * y asignándole el nuevo Pago.
     */
    private DetallePago clonarDetallePago(DetallePago original, Pago nuevoPago) {
        DetallePago clone = new DetallePago();
        // Copiamos las propiedades básicas y de asociación
        clone.setDescripcionConcepto(original.getDescripcionConcepto());
        clone.setConcepto(original.getConcepto());
        clone.setSubConcepto(original.getSubConcepto());
        clone.setCuotaOCantidad(original.getCuotaOCantidad());
        clone.setBonificacion(original.getBonificacion());
        clone.setRecargo(original.getRecargo());
        clone.setValorBase(original.getValorBase());
        clone.setTipo(original.getTipo());
        clone.setFechaRegistro(original.getFechaRegistro());

        // IMPORTANTE: Ajuste de importes
        // Se toma el importe pendiente actual del detalle original y se usa para iniciar el nuevo detalle.
        double pendiente = (original.getImportePendiente() != null) ? original.getImportePendiente() : 0.0;
        clone.setImporteInicial(pendiente);
        clone.setImportePendiente(pendiente);

        // Se asigna el alumno del nuevo pago
        clone.setAlumno(nuevoPago.getAlumno());

        // Copiamos las asociaciones restantes
        clone.setMensualidad(original.getMensualidad());
        clone.setMatricula(original.getMatricula());
        clone.setStock(original.getStock());

        // El nuevo detalle siempre inicia sin estar cobrado
        clone.setCobrado(false);

        // Se asigna el nuevo pago al clon
        clone.setPago(nuevoPago);

        return clone;
    }

    /**
     * Busca un DetallePago existente en base a criterios únicos:
     * - Alumno (a través del pago)
     * - Descripción normalizada
     * - Tipo (MENSUALIDAD o MATRICULA)
     * - Asociación con matrícula o mensualidad (según corresponda)
     * - Estado: cobrado = false
     *
     * @param detalle  el detalle de pago a evaluar.
     * @param alumnoId el ID del alumno asociado.
     * @return el DetallePago encontrado o null si no existe.
     */
    public DetallePago findDetallePagoByCriteria(DetallePago detalle, Long alumnoId) {
        // Se utiliza la descripción tal como se guarda en la BD (normalizada en la entidad)
        String descripcion = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto().trim()
                : null;

        Long matriculaId = (detalle.getMatricula() != null) ? detalle.getMatricula().getId() : null;
        Long mensualidadId = (detalle.getMensualidad() != null) ? detalle.getMensualidad().getId() : null;
        TipoDetallePago tipo = detalle.getTipo();

        log.info("Buscando DetallePago para alumnoId={}, descripcion='{}', tipo={}, matriculaId={}, mensualidadId={}",
                alumnoId, descripcion, tipo,
                (matriculaId != null ? matriculaId : "null"),
                (mensualidadId != null ? mensualidadId : "null"));

        if (matriculaId != null) {
            return detallePagoRepositorio
                    .findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalseAndMatricula_Id(
                            alumnoId, descripcion, tipo, matriculaId)
                    .orElse(null);
        } else if (mensualidadId != null) {
            return detallePagoRepositorio
                    .findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalseAndMensualidad_Id(
                            alumnoId, descripcion, tipo, mensualidadId)
                    .orElse(null);
        } else {
            return detallePagoRepositorio
                    .findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalse(
                            alumnoId, descripcion, tipo)
                    .orElse(null);
        }
    }

}
