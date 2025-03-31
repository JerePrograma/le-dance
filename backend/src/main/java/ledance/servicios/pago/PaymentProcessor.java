package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.detallepago.DetallePagoServicio;
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
 * Se ha consolidado la logica de procesamiento de cada DetallePago en un unico metodo:
 * - Se elimina la duplicidad entre procesarDetallePago y calcularImporte, centralizando el flujo en
 * {@code procesarYCalcularDetalle(Pago, DetallePago)}.
 * - Se centraliza el cálculo del abono y la actualizacion de importes en el metodo
 * {@code procesarAbono(...)}.
 * - La determinacion del tipo de detalle se realiza siempre mediante {@code determinarTipoDetalle(...)}.
 * - Se diferencia claramente entre el caso de pago nuevo (donde se clona el detalle si ya existe en BD)
 * y el de actualizacion (se carga el detalle persistido y se actualizan sus campos).
 * - Finalmente, se asegura que al finalizar el procesamiento de cada detalle se actualicen los totales
 * del pago y se verifiquen los estados relacionados (por ejemplo, marcar mensualidad o matricula como
 * pagada, o reducir el stock).
 * -------------------------------------------------------------------------------------------------
 */
@Service
@Transactional
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoServicio detallePagoServicio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final RecargoRepositorio recargoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final UsuarioRepositorio usuarioRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio, DetallePagoRepositorio detallePagoRepositorio, DetallePagoServicio detallePagoServicio, BonificacionRepositorio bonificacionRepositorio, RecargoRepositorio recargoRepositorio, MetodoPagoRepositorio metodoPagoRepositorio, PaymentCalculationServicio paymentCalculationServicio, UsuarioRepositorio usuarioRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional
    public void recalcularTotales(Pago pago) {
        log.info("[recalcularTotales] Recalculo iniciando para Pago ID: {}", pago.getId());

        // Inicialización de acumuladores.
        BigDecimal totalACobrar = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        log.info("[recalcularTotales] Inicializando acumuladores: totalACobrar={}, totalPendiente={}",
                totalACobrar, totalPendiente);

        // Procesamiento de cada detalle del pago.
        for (DetallePago detalle : pago.getDetallePagos()) {
            log.info("[recalcularTotales] Procesando detalle ID: {}", detalle.getId());

            // Aseguramos que las asociaciones estén asignadas.
            detalle.setConcepto(detalle.getConcepto());
            log.info("[recalcularTotales] Concepto asignado en detalle ID {}: {}", detalle.getId(), detalle.getConcepto());
            detalle.setSubConcepto(detalle.getSubConcepto());
            log.info("[recalcularTotales] SubConcepto asignado en detalle ID {}: {}", detalle.getId(), detalle.getSubConcepto());
            detalle.setAlumno(pago.getAlumno());
            log.info("[recalcularTotales] Alumno asignado al detalle ID {}: Alumno ID={}", detalle.getId(), pago.getAlumno().getId());

            // Conversión de "aCobrar" a BigDecimal.
            BigDecimal aCobrar = Optional.ofNullable(detalle.getaCobrar())
                    .map(BigDecimal::valueOf)
                    .orElse(BigDecimal.ZERO);
            log.info("[recalcularTotales] Valor a cobrar para detalle ID {}: {}", detalle.getId(), aCobrar);

            // Conversión de "importePendiente" a BigDecimal.
            BigDecimal impPendiente = Optional.ofNullable(detalle.getImportePendiente())
                    .map(BigDecimal::valueOf)
                    .orElse(BigDecimal.ZERO);
            log.info("[recalcularTotales] Importe pendiente para detalle ID {}: {}", detalle.getId(), impPendiente);

            // Acumulación de totales.
            totalACobrar = totalACobrar.add(aCobrar);
            totalPendiente = totalPendiente.add(impPendiente);
            log.info("[recalcularTotales] Acumulado hasta detalle ID {}: totalACobrar={}, totalPendiente={}",
                    detalle.getId(), totalACobrar, totalPendiente);
        }

        // Determinar si se aplica crédito (saldo a favor) en caso de matrícula.
        boolean aplicarCredito = pago.getDetallePagos().stream()
                .anyMatch(det -> det.getDescripcionConcepto() != null &&
                        det.getDescripcionConcepto().toLowerCase().contains("matrícula"));
        log.info("[recalcularTotales] Aplicar crédito (saldo a favor) en matrícula: {}", aplicarCredito);

        // Determinar si se debe aplicar recargo según el método de pago.
        boolean aplicarRecargoMetodo = pago.getDetallePagos().stream()
                .anyMatch(det -> det.getTieneRecargo() && (pago.getMetodoPago() != null));
        log.info("[recalcularTotales] Se detecta recargo en método de pago: {}", aplicarRecargoMetodo);

        double montoRecargo = 0;
        if (aplicarRecargoMetodo) {
            montoRecargo = pago.getMetodoPago().getRecargo();
            log.info("[recalcularTotales] Monto de recargo obtenido: {}", montoRecargo);
        } else {
            log.info("[recalcularTotales] No se aplica recargo por método de pago.");
        }

        // Calcular el monto final sumando totalACobrar y el recargo.
        double montoFinal = totalACobrar.doubleValue() + montoRecargo;
        log.info("[recalcularTotales] Monto final previo al crédito: {}", montoFinal);

        // Aplicar saldo a favor si corresponde.
        double saldoAFavor = pago.getAlumno().getCreditoAcumulado();
        if (aplicarCredito) {
            log.info("[recalcularTotales] Aplicando saldo a favor: {}", saldoAFavor);
            montoFinal -= saldoAFavor;
            log.info("[recalcularTotales] Monto final después de aplicar saldo a favor: {}", montoFinal);
        } else {
            log.info("[recalcularTotales] No se aplica saldo a favor, no es matrícula.");
        }

        // Asignar monto y montoPagado al pago.
        pago.setMonto(montoFinal);
        log.info("[recalcularTotales] Asignado monto al pago: {}", montoFinal);
        pago.setMontoPagado(montoFinal);
        log.info("[recalcularTotales] Asignado montoPagado al pago: {}", montoFinal);

        // El saldo restante es la suma total de "importePendiente" de cada detalle.
        BigDecimal saldoRestante = totalPendiente;
        log.info("[recalcularTotales] Saldo restante calculado antes de ajuste: {}", saldoRestante);

        // Ajuste del estado del pago según el saldo restante.
        if (saldoRestante.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[recalcularTotales] Saldo restante <= 0, ajustando a 0 y marcando como HISTÓRICO");
            saldoRestante = BigDecimal.ZERO;
            pago.setEstadoPago(EstadoPago.HISTORICO);
        } else {
            log.info("[recalcularTotales] Saldo restante positivo, marcando como ACTIVO");
            pago.setEstadoPago(EstadoPago.ACTIVO);
        }
        pago.setSaldoRestante(saldoRestante.doubleValue());
        log.info("[recalcularTotales] Saldo restante final asignado: {}", saldoRestante.doubleValue());

        // Para pagos nuevos (sin ID), se reinician monto y montoPagado.
        if (pago.getId() == null) {
            pago.setMonto(0.0);
            pago.setMontoPagado(0.0);
            log.info("[recalcularTotales] Pago nuevo detectado (ID nulo), se asigna monto y montoPagado = 0");
        }

        log.info("[recalcularTotales] Finalizado para Pago ID: {}: Monto={}, Pagado={}, SaldoRestante={}, Estado={}",
                pago.getId(), pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante(), pago.getEstadoPago());
    }

    /**
     * Obtiene la inscripcion asociada al detalle, si aplica.
     *
     * @param detalle el objeto DetallePago.
     * @return la Inscripcion asociada o null.
     */
    public Inscripcion obtenerInscripcion(DetallePago detalle) {
        log.info("[obtenerInscripcion] Buscando inscripción para DetallePago id={}", detalle.getId());
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getInscripcion() != null) {
            return detalle.getMensualidad().getInscripcion();
        }
        return null;
    }

    /**
     * Carga el pago existente desde la base de datos y actualiza sus campos basicos.
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
        pagoManaged.setAlumno(pago.getAlumno());
        return pagoManaged;
    }

    // 1. Obtener el ultimo pago pendiente (se mantiene similar, verificando saldo > 0)
    @Transactional
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        log.info("[obtenerUltimoPagoPendienteEntidad] Buscando el último pago pendiente para alumnoId={}", alumnoId);
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

    /**
     * Verifica que el saldo restante no sea negativo. Si es negativo, lo ajusta a 0.
     */
    @Transactional
    void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.", pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}", pago.getId(), pago.getSaldoRestante());
        }
    }

    @Transactional
    public Pago actualizarPagoHistoricoConAbonos(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago id={} con abonos", pagoHistorico.getId());

        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            // Normalizamos la descripción para evitar conflictos de espacios o mayúsculas
            String descripcionNormalizada = detalleReq.descripcionConcepto().trim().toUpperCase();
            log.info("[actualizarPagoHistoricoConAbonos] Procesando detalle del request: '{}'", descripcionNormalizada);

            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto() != null &&
                            d.getDescripcionConcepto().trim().equalsIgnoreCase(descripcionNormalizada) &&
                            d.getImportePendiente() != null &&
                            d.getImportePendiente() > 0.0 &&
                            !d.getCobrado())
                    .findFirst();

            if (detalleOpt.isPresent()) {
                DetallePago detalleExistente = detalleOpt.get();
                if (!detalleExistente.getTieneRecargo()) {
                    detalleExistente.setRecargo(null);
                    detalleExistente.setTieneRecargo(false);
                }
                log.info("[actualizarPagoHistoricoConAbonos] Detalle existente encontrado id={} para '{}'", detalleExistente.getId(), descripcionNormalizada);
                log.info("[actualizarPagoHistoricoConAbonos] Actualizando aCobrar del detalle id={} (acumulado: {})",
                        detalleExistente.getId(), detalleReq.aCobrar());
                detalleExistente.setaCobrar(detalleReq.aCobrar());
                procesarDetalle(pagoHistorico, detalleExistente, pagoHistorico.getAlumno());
            } else {
                log.info("[actualizarPagoHistoricoConAbonos] No se encontró detalle existente para '{}'. Creando nuevo detalle.", descripcionNormalizada);
                DetallePago nuevoDetalle = crearNuevoDetalleFromRequest(detalleReq, pagoHistorico);
                log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle creado: '{}', importePendiente={}",
                        nuevoDetalle.getDescripcionConcepto(), nuevoDetalle.getImportePendiente());
                if (nuevoDetalle.getImportePendiente() > 0) {
                    pagoHistorico.getDetallePagos().add(nuevoDetalle);
                    procesarDetalle(pagoHistorico, nuevoDetalle, pagoHistorico.getAlumno());
                } else {
                    log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle '{}' sin importe pendiente, se omite.", nuevoDetalle.getDescripcionConcepto());
                }
            }
        }

        log.info("[actualizarPagoHistoricoConAbonos] Recalculando totales del pago id={}", pagoHistorico.getId());
        recalcularTotales(pagoHistorico);
        pagoHistorico = pagoRepositorio.save(pagoHistorico);
        log.info("[actualizarPagoHistoricoConAbonos] Pago id={} actualizado. Totales: monto={}, saldoRestante={}",
                pagoHistorico.getId(), pagoHistorico.getMonto(), pagoHistorico.getSaldoRestante());
        return pagoHistorico;
    }

    /**
     * Procesa un detalle individual: asigna alumno y pago, reatacha asociaciones y llama a la lógica
     * de procesamiento y cálculo de detalle.
     */
    private void procesarDetalle(Pago pago, DetallePago detalle, Alumno alumnoPersistido) {
        log.info("[procesarDetalle] INICIO - Procesando DetallePago id={} para Pago id={} (Alumno id={})",
                detalle.getId(),
                pago.getId(),
                alumnoPersistido.getId());
        log.info("[procesarDetalle] Iniciando cálculo DetallePago: {}", detalle);
        if (detalle.getTipo().equals(TipoDetallePago.STOCK) || detalle.getTipo().equals(TipoDetallePago.CONCEPTO)) {
            detalle.setImporteInicial(detalle.getImportePendiente());
        }
        // 1. Asignación de relaciones
        log.info("[procesarDetalle] Asignando alumno persistido (id={}) al detalle", alumnoPersistido.getId());
        detalle.setAlumno(alumnoPersistido);
        log.debug("[procesarDetalle] Alumno asignado verificado: {}", detalle.getAlumno().getId());

        log.info("[procesarDetalle] Asignando pago (id={}) al detalle", pago.getId());
        detalle.setPago(pago);
        log.debug("[procesarDetalle] Pago asignado verificado: {}", detalle.getPago().getId());

        // 2. Ajuste de recargo
        log.info("[procesarDetalle] Verificando recargo. TieneRecargo={}", detalle.getTieneRecargo());
        if (!detalle.getTieneRecargo() && detalle.getMensualidad() != null || detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
            log.info("[procesarDetalle] Sin recargo - Estableciendo recargo=null");
            detalle.setRecargo(null);
            detalle.setTieneRecargo(false);
            log.debug("[procesarDetalle] Recargo verificado: {}", detalle.getRecargo());
        } else {
            log.info("[procesarDetalle] Manteniendo recargo existente: {}", detalle.getRecargo());
        }

        // 3. Persistencia condicional del pago
        log.info("[procesarDetalle] Verificando persistencia del pago. ID actual={}", pago.getId());
        if (pago.getId() == null) {
            log.info("[procesarDetalle] Persistiendo pago nuevo");
            entityManager.persist(pago);
            entityManager.flush();
            log.info("[procesarDetalle] Pago persistido - Nuevo ID generado: {}", pago.getId());
        }

        // 4. Reattach de asociaciones
        log.info("[procesarDetalle] Reattachando asociaciones para detalle id={}", detalle.getId());
        paymentCalculationServicio.reatacharAsociaciones(detalle, pago);

        // 5. Obtención de inscripción
        log.info("[procesarDetalle] Buscando inscripción asociada al detalle");
        Inscripcion inscripcion = obtenerInscripcion(detalle);
        log.info("[procesarDetalle] Inscripción {} encontrada: {}",
                (inscripcion != null ? "id=" + inscripcion.getId() : "no"),
                (inscripcion != null ? inscripcion.toString() : "N/A"));
        log.info("[procesarDetalle] Detalle procesado - Estado inicial: Cobrado={}, aCobrar={}, Pendiente={}",
                detalle.getCobrado(),
                detalle.getaCobrar(),
                detalle.getImportePendiente());
        // 6. Procesamiento principal
        log.info("[procesarDetalle] Invocando procesarYCalcularDetalle para detalle id={}", detalle.getId());
        paymentCalculationServicio.procesarYCalcularDetalle(detalle, inscripcion);
        log.info("[procesarDetalle] Detalle procesado - Estado final: Cobrado={}, aCobrar={}, Pendiente={}",
                detalle.getCobrado(),
                detalle.getaCobrar(),
                detalle.getImportePendiente());

        log.info("[procesarDetalle] FIN - Procesamiento completado para DetallePago id={} (Pago id={})",
                detalle.getId(),
                pago.getId());
        log.info("[procesarDetalle] Finalizando cálculo DetallePago: {}", detalle);
    }

    /**
     * Refactor de crearNuevoDetalleFromRequest:
     * - Se normaliza la descripción y se determinan las asociaciones.
     * - Se asignan el alumno, el pago y se configuran las propiedades base.
     * - Se invoca el cálculo de importes para establecer los valores finales.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] INICIO - Creando detalle desde request. Pago ID: {}", pago.getId());
        log.debug("[crearNuevoDetalleFromRequest] Request recibido: {}", req.toString());

        // 1. Creación de instancia
        log.info("[crearNuevoDetalleFromRequest] Creando nueva instancia de DetallePago");
        DetallePago detalle = new DetallePago();
        log.debug("[crearNuevoDetalleFromRequest] Detalle creado (sin persistir): {}", detalle.toString());

        // 2. Manejo de ID
        log.info("[crearNuevoDetalleFromRequest] Procesando ID del request: {}", req.id());
        if (req.id() == 0) {
            log.info("[crearNuevoDetalleFromRequest] ID=0 recibido - Asignando null para generación automática");
            detalle.setId(null);
        } else {
            log.warn("[crearNuevoDetalleFromRequest] ID no cero recibido ({}) - Posible intento de modificación directa", req.id());
        }

        // 3. Asignación de relaciones principales
        log.info("[crearNuevoDetalleFromRequest] Asignando alumno (ID: {}) y pago (ID: {})",
                pago.getAlumno().getId(), pago.getId());
        detalle.setAlumno(pago.getAlumno());
        detalle.setPago(pago);
        log.debug("[crearNuevoDetalleFromRequest] Relaciones asignadas - Alumno: {}, Pago: {}",
                detalle.getAlumno().getId(), detalle.getPago().getId());

        // 4. Normalización de descripción
        log.info("[crearNuevoDetalleFromRequest] Normalizando descripción: '{}'", req.descripcionConcepto());
        String descripcion = req.descripcionConcepto().trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[crearNuevoDetalleFromRequest] Descripción normalizada asignada: '{}'", detalle.getDescripcionConcepto());

        // 5. Asignación de valores base
        log.info("[crearNuevoDetalleFromRequest] Asignando valores base - Valor: {}, Cuota/Cantidad: {}",
                req.valorBase(), req.cuotaOCantidad());
        detalle.setValorBase(req.valorBase());
        detalle.setCuotaOCantidad(req.cuotaOCantidad());
        log.debug("[crearNuevoDetalleFromRequest] Valores base asignados - ValorBase: {}, Cuota: {}",
                detalle.getValorBase(), detalle.getCuotaOCantidad());

        // 6. Determinación de tipo
        log.info("[crearNuevoDetalleFromRequest] Determinando tipo de detalle para descripción: '{}'", descripcion);
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[crearNuevoDetalleFromRequest] Tipo asignado: {}", detalle.getTipo());

        // 7. Manejo de bonificación
        log.info("[crearNuevoDetalleFromRequest] Procesando bonificación - ID solicitado: {}", req.bonificacionId());
        if (req.bonificacionId() != null) {
            log.info("[crearNuevoDetalleFromRequest] Buscando bonificación con ID: {}", req.bonificacionId());
            Bonificacion bonificacion = obtenerBonificacionPorId(req.bonificacionId());
            detalle.setBonificacion(bonificacion);
            log.info("[crearNuevoDetalleFromRequest] Bonificación asignada - ID: {}, Descripción: {}",
                    bonificacion.getId(), bonificacion.getDescripcion());
        }

        // 8. Manejo de recargo
        log.info("[crearNuevoDetalleFromRequest] Procesando recargo - TieneRecargo: {}, RecargoID: {}",
                req.tieneRecargo(), req.recargoId());
        if (req.tieneRecargo()) {
            if (req.recargoId() != null) {
                log.info("[crearNuevoDetalleFromRequest] Buscando recargo con ID: {}", req.recargoId());
                Recargo recargo = obtenerRecargoPorId(req.recargoId());
                detalle.setRecargo(recargo);
                log.info("[crearNuevoDetalleFromRequest] Recargo asignado - ID: {}, Porcentaje: {}%",
                        recargo.getId(), recargo.getPorcentaje());
            } else {
                log.warn("[crearNuevoDetalleFromRequest] Flag tieneRecargo=true pero sin recargoId especificado");
            }
        } else {
            log.info("[crearNuevoDetalleFromRequest] No se asigna recargo (tieneRecargo=false o nulo)");
            detalle.setRecargo(null);
            detalle.setTieneRecargo(false);
            detalle.setImportePendiente(detalle.getImporteInicial());
        }

        // 9. Cálculo de importes
        log.info("[crearNuevoDetalleFromRequest] Invocando cálculo de importes");
        detallePagoServicio.calcularImporte(detalle);
        // Opcional: Forzar o validar los valores enviados desde el request
        if (req.aCobrar() > 0) {
            log.info("[crearNuevoDetalleFromRequest] Forzando valor aCobrar desde request: {}", req.aCobrar());
            detalle.setaCobrar(req.aCobrar());
        }
        if (req.importePendiente() > 0) {
            log.info("[crearNuevoDetalleFromRequest] Forzando importePendiente desde request: {}", req.importePendiente());
            if (req.tieneRecargo()) {
                detalle.setImportePendiente(req.importePendiente());
            } else {
                detalle.setImportePendiente(req.importePendiente());
            }
        }

        // 10. Estado de cobro
        boolean cobrado = detalle.getImportePendiente() <= 0.0;
        log.info("[crearNuevoDetalleFromRequest] Determinando estado de cobro - Pendiente: {} → Cobrado: {}",
                detalle.getImportePendiente(), cobrado);
        detalle.setCobrado(cobrado);

        log.info("[crearNuevoDetalleFromRequest] FIN - Detalle creado exitosamente. ID: {}, Tipo: {}, Cobrado: {}",
                (detalle.getId() != null ? detalle.getId() : "NUEVO"),
                detalle.getTipo(),
                detalle.getCobrado());
        log.debug("[crearNuevoDetalleFromRequest] Detalle completo: {}", detalle.toString());

        return detalle;
    }

    /**
     * Métodos auxiliares para obtener Bonificación y Recargo (suponiendo repositorios adecuados).
     */
    private Bonificacion obtenerBonificacionPorId(Long id) {
        return bonificacionRepositorio.findById(id).orElse(null);
    }

    private Recargo obtenerRecargoPorId(Long id) {
        return recargoRepositorio.findById(id).orElse(null);
    }

    @Transactional
    public Pago clonarDetallesConPendiente(Pago pagoHistorico) {
        log.info("[clonarDetallesConPendiente] INICIO - Clonando detalles pendientes para pago histórico ID: {}", pagoHistorico.getId());

        // 1. Creación del nuevo pago y copia de datos básicos
        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        nuevoPago.setFecha(LocalDate.now());
        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        nuevoPago.setDetallePagos(new ArrayList<>());
        nuevoPago.setMetodoPago(pagoHistorico.getMetodoPago());
        log.info("[clonarDetallesConPendiente] Datos básicos del nuevo pago copiados.");
        Usuario cobrador = pagoHistorico.getUsuario();
        // 2. Procesamiento y clonación de detalles pendientes
        int detallesClonados = 0;
        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            detalle.setUsuario(pagoHistorico.getUsuario());
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0.0) {
                if (detalle.getTipo().equals(TipoDetallePago.MATRICULA)) {
                    Alumno alumno = detalle.getAlumno();
                    if (alumno.getCreditoAcumulado() > 0) {
                        detalle.setImportePendiente(detalle.getImportePendiente() - alumno.getCreditoAcumulado());
                        alumno.setCreditoAcumulado(0.0);
                        detalle.setAlumno(alumno);
                    }
                }
                log.info("[clonarDetallesConPendiente] Clonando detalle ID: {} con pendiente: {}",
                        detalle.getId(), detalle.getImportePendiente());
                DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);

                nuevoDetalle.setUsuario(cobrador);
                // Manejo de recargos: limpiar si tieneRecargo es false
                if (!detalle.getTieneRecargo() && detalle.getMensualidad() != null || detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
                    nuevoDetalle.setRecargo(null);
                    nuevoDetalle.setTieneRecargo(false);
                    log.debug("[clonarDetallesConPendiente] Recargo limpiado en detalle clonado ID: {}", nuevoDetalle.getId());
                }

                // Reatachar mensualidad, si existe
                if (detalle.getMensualidad() != null) {
                    detalle.getMensualidad().setEsClon(true);
                    nuevoDetalle.setMensualidad(detalle.getMensualidad());
                    log.debug("[clonarDetallesConPendiente] Mensualidad reatachada en detalle clonado ID: {}", nuevoDetalle.getId());
                }
                nuevoDetalle.setTipo(detalle.getTipo());
                if (nuevoDetalle.getConcepto() != null && nuevoDetalle.getSubConcepto() == null) {
                    nuevoDetalle.setSubConcepto(nuevoDetalle.getConcepto().getSubConcepto());
                }
                nuevoDetalle.setaCobrar(detalle.getaCobrar());

                nuevoPago.getDetallePagos().add(nuevoDetalle);
                detallesClonados++;
                log.info("[clonarDetallesConPendiente] Detalle clonado exitosamente. Total clonados: {}", detallesClonados);
            } else {
                log.debug("[clonarDetallesConPendiente] Detalle ID {} omitido (sin pendiente)", detalle.getId());
            }
        }

        // 3. Validación: si no hay detalles pendientes, retornar null
        if (nuevoPago.getDetallePagos().isEmpty()) {
            log.warn("[clonarDetallesConPendiente] FIN - No se clonaron detalles (todos estaban saldados)");
            return null;
        }

        // 4. Cálculos finales y actualización de totales
        recalcularTotales(nuevoPago);
        double importeInicial = calcularImporteInicialDesdeDetalles(nuevoPago.getDetallePagos());
        nuevoPago.setImporteInicial(importeInicial);
        log.info("[clonarDetallesConPendiente] Totales recalculados. Importe inicial calculado: {}", importeInicial);
        nuevoPago.setUsuario(cobrador);
        // 5. Persistencia y retorno del nuevo pago
        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] FIN - Nuevo pago creado con éxito. Detalles: {} | Nuevo ID: {}",
                nuevoPago.getDetallePagos().size(), nuevoPago.getId());
        return nuevoPago;
    }

    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo") Double calcularImporteInicialDesdeDetalles(List<DetallePago> detallePagos) {
        log.info("[calcularImporteInicialDesdeDetalles] Iniciando calculo del importe inicial.");

        if (detallePagos == null || detallePagos.isEmpty()) {
            log.info("[calcularImporteInicialDesdeDetalles] Lista de DetallePagos nula o vacia. Retornando 0.0");
            return 0.0;
        }

        double total = detallePagos.stream().filter(Objects::nonNull).mapToDouble(detalle -> Optional.ofNullable(detalle.getImporteInicial()).orElse(0.0)).sum();

        total = Math.max(0.0, total); // Asegura que no sea negativo
        log.info("[calcularImporteInicialDesdeDetalles] Total calculado: {}", total);

        return total;
    }

    /**
     * Metodo auxiliar para clonar un DetallePago, copiando todos los campos excepto id y version,
     * y asignandole el nuevo Pago.
     */
    // Método corregido para clonar detalle considerando correctamente el pendiente
    private DetallePago clonarDetallePago(DetallePago original, Pago nuevoPago) {
        log.info("[clonarDetallePago] Iniciando clonación de DetallePago. Original ID: {}", original.getId());

        // Creación del clon
        log.debug("[clonarDetallePago] Creando nueva instancia de DetallePago para el clon");
        DetallePago clone = new DetallePago();

        // Marcado de clon/original
        log.debug("[clonarDetallePago] Configurando flags de clonación: clone.esClon=true, original.esClon=false");
        clone.setEsClon(true);
        original.setEsClon(false);

        // Copia de atributos básicos
        log.debug("[clonarDetallePago] Copiando descripciónConcepto: {}", original.getDescripcionConcepto());
        clone.setDescripcionConcepto(original.getDescripcionConcepto());

        // Manejo de Concepto y SubConcepto
        log.debug("[clonarDetallePago] Procesando Concepto y SubConcepto...");
        if (original.getConcepto() != null && original.getConcepto().getId() != null) {
            log.debug("[clonarDetallePago] Concepto original tiene ID: {}", original.getConcepto().getId());
            Concepto managedConcepto = entityManager.find(Concepto.class, original.getConcepto().getId());

            if (managedConcepto != null) {
                log.debug("[clonarDetallePago] Concepto encontrado en EntityManager, reattachando...");
                clone.setConcepto(managedConcepto);
                log.info("[clonarDetallePago] Concepto reatachado en el clon: {}", managedConcepto.getId());

                if (original.getSubConcepto() != null) {
                    log.debug("[clonarDetallePago] SubConcepto original presente, copiando...");
                    clone.setSubConcepto(managedConcepto.getSubConcepto());
                    log.info("[clonarDetallePago] SubConcepto reatachado en el clon: {}", managedConcepto.getSubConcepto().getId());
                } else {
                    log.debug("[clonarDetallePago] No hay SubConcepto en el original");
                }
            } else {
                log.warn("[clonarDetallePago] Concepto no encontrado en EntityManager, copiando referencia directa");
                clone.setConcepto(original.getConcepto());
                clone.setSubConcepto(original.getSubConcepto());
            }
        } else {
            log.debug("[clonarDetallePago] Concepto original es null o sin ID, copiando referencia directa");
            clone.setConcepto(original.getConcepto());
            clone.setSubConcepto(original.getSubConcepto());
        }

        // Copia de atributos numéricos y booleanos
        log.debug("[clonarDetallePago] Copiando cuotaOCantidad: {}", original.getCuotaOCantidad());
        clone.setCuotaOCantidad(original.getCuotaOCantidad());

        log.debug("[clonarDetallePago] Copiando bonificacion: {}", original.getBonificacion());
        clone.setBonificacion(original.getBonificacion());

        // Manejo de recargo
        if (original.getTieneRecargo()) {
            log.debug("[clonarDetallePago] Original tiene recargo: {}, copiando...", original.getRecargo());
            clone.setRecargo(original.getRecargo());
        } else {
            log.debug("[clonarDetallePago] Original no tiene recargo, configurando clone.recargo=null y tieneRecargo=false");
            clone.setRecargo(null);
            clone.setTieneRecargo(false);
        }

        log.debug("[clonarDetallePago] Copiando valorBase: {}", original.getValorBase());
        clone.setValorBase(original.getValorBase());

        log.debug("[clonarDetallePago] Copiando tipo: {}", original.getTipo());
        clone.setTipo(original.getTipo());

        log.debug("[clonarDetallePago] Estableciendo fechaRegistro a hoy");
        clone.setFechaRegistro(LocalDate.now());

        // Cálculo de importes
        double importePendienteRestante = original.getImportePendiente() != null ?
                original.getImportePendiente() : original.getImporteInicial();
        log.debug("[clonarDetallePago] importePendienteRestante calculado: {}", importePendienteRestante);

        log.debug("[clonarDetallePago] Configurando importeInicial: {}", importePendienteRestante);
        clone.setImporteInicial(importePendienteRestante);

        log.debug("[clonarDetallePago] Configurando importePendiente: {}", importePendienteRestante);
        clone.setImportePendiente(importePendienteRestante);

        log.debug("[clonarDetallePago] Asignando alumno del nuevo pago");
        clone.setAlumno(nuevoPago.getAlumno());

        log.debug("[clonarDetallePago] Copiando aCobrar: {}", original.getaCobrar());
        clone.setaCobrar(original.getaCobrar());

        // Manejo de Mensualidad
        log.debug("[clonarDetallePago] Procesando mensualidad...");
        Mensualidad originalMensualidad = original.getMensualidad();
        if (originalMensualidad != null) {
            log.debug("[clonarDetallePago] Mensualidad presente en original, marcando como clon y copiando");
            originalMensualidad.setEsClon(true);
            originalMensualidad.setDescripcion(originalMensualidad.getDescripcion());
            clone.setMensualidad(originalMensualidad);
        } else {
            log.debug("[clonarDetallePago] No hay mensualidad en el original");
            clone.setMensualidad(null);
        }

        // Copia de atributos restantes
        log.debug("[clonarDetallePago] Copiando matricula");
        clone.setMatricula(original.getMatricula());

        log.debug("[clonarDetallePago] Copiando stock: {}", original.getStock());
        clone.setStock(original.getStock());

        boolean cobrado = importePendienteRestante == 0;
        log.debug("[clonarDetallePago] Configurando cobrado: {}", cobrado);
        clone.setCobrado(cobrado);

        log.debug("[clonarDetallePago] Asignando nuevo pago al clon");
        clone.setPago(nuevoPago);

        log.info("[clonarDetallePago] Clonación completada exitosamente para DetallePago original ID: {}", original.getId());
        return clone;
    }

    /**
     * Refactor de findDetallePagoByCriteria (se mantiene para compatibilidad, aunque la fusión se maneja con el mapa).
     * Busca un DetallePago basado en descripción, tipo y, si aplica, el id de matrícula o mensualidad.
     */
    public DetallePago findDetallePagoByCriteria(DetallePago detalle, Long alumnoId) {
        String descripcion = (detalle.getDescripcionConcepto() != null) ? detalle.getDescripcionConcepto().trim().toUpperCase() : "";
        Long matriculaId = (detalle.getMatricula() != null) ? detalle.getMatricula().getId() : null;
        Long mensualidadId = (detalle.getMensualidad() != null) ? detalle.getMensualidad().getId() : null;
        TipoDetallePago tipo = detalle.getTipo();
        log.info("Buscando DetallePago para alumnoId={}, descripción='{}', tipo={}, matriculaId={}, mensualidadId={}", alumnoId, descripcion, tipo, (matriculaId != null ? matriculaId : "null"), (mensualidadId != null ? mensualidadId : "null"));
        if (matriculaId != null) {
            return detallePagoRepositorio.findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalseAndMatricula_Id(alumnoId, descripcion, tipo, matriculaId).orElse(null);
        } else if (mensualidadId != null) {
            return detallePagoRepositorio.findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalseAndMensualidad_Id(alumnoId, descripcion, tipo, mensualidadId).orElse(null);
        } else {
            return detallePagoRepositorio.findByPago_Alumno_IdAndDescripcionConceptoIgnoreCaseAndTipoAndCobradoFalse(alumnoId, descripcion, tipo).orElse(null);
        }
    }

    /**
     * Procesa los detalles de pago: asigna alumno y pago a cada DetallePago,
     * separa los detalles ya persistidos de los nuevos, reatacha las asociaciones y recalcula totales.
     */
    @Transactional
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumnoPersistido) {

        log.info("[processDetallesPago] INICIO. Pago ID={}, Alumno ID={}", pago.getId(), alumnoPersistido.getId());

        pago.setAlumno(alumnoPersistido);

        if (pago.getDetallePagos() == null) {
            pago.setDetallePagos(new ArrayList<>());
            log.info("[processDetallesPago] detallePagos inicializado como nueva lista.");
        } else {
            pago.getDetallePagos().clear();
            log.info("[processDetallesPago] detallePagos limpiado.");
        }

        List<DetallePago> detallesProcesados = new ArrayList<>();

        for (DetallePago detalleRequest : detallesFront) {
            detalleRequest.setAlumno(alumnoPersistido);
            detalleRequest.setPago(pago);
            detalleRequest.setConcepto(detalleRequest.getConcepto());
            detalleRequest.setSubConcepto(detalleRequest.getSubConcepto());
            DetallePago detallePersistido = null;
            detalleRequest.setUsuario(pago.getUsuario());
            if (detalleRequest.getId() != null && detalleRequest.getId() > 0) {
                detallePersistido = detallePagoRepositorio.findById(detalleRequest.getId()).orElse(null);
                log.info("[processDetallesPago] Buscando detallePersistido con ID={}: Encontrado={}", detalleRequest.getId(), detallePersistido != null);
            }
            if (!detalleRequest.getTieneRecargo()) {
                detalleRequest.setRecargo(null);
                detalleRequest.setTieneRecargo(false);
            }

            if (detallePersistido != null) {
                detallePersistido.setUsuario(pago.getUsuario());
                actualizarDetalleDesdeRequest(detallePersistido, detalleRequest);
                procesarDetalle(pago, detallePersistido, alumnoPersistido);
                detallesProcesados.add(detallePersistido);
                log.info("[processDetallesPago] Detalle existente procesado ID={}", detallePersistido.getId());
            } else {
                DetallePago nuevoDetalle = new DetallePago();
                copiarAtributosDetalle(nuevoDetalle, detalleRequest);
                nuevoDetalle.setFechaRegistro(LocalDate.now());
                TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(nuevoDetalle.getDescripcionConcepto());
                nuevoDetalle.setTipo(tipo);
                procesarDetalle(pago, nuevoDetalle, alumnoPersistido);
                nuevoDetalle.setUsuario(pago.getUsuario());
                detallesProcesados.add(nuevoDetalle);
                log.info("[processDetallesPago] Nuevo detalle creado y procesado");
            }
        }

        pago.getDetallePagos().addAll(detallesProcesados);

        Pago pagoPersistido;
        if (pago.getId() == null) {
            entityManager.persist(pago);
            pagoPersistido = pago;
            log.info("[processDetallesPago] Pago persistido con ID={}", pagoPersistido.getId());
        } else {
            pagoPersistido = entityManager.merge(pago);
            log.info("[processDetallesPago] Pago actualizado con ID={}", pagoPersistido.getId());
        }

        entityManager.flush();

        recalcularTotales(pagoPersistido);

        log.info("[processDetallesPago] FIN. Pago ID={}, Monto={}, SaldoRestante={}", pagoPersistido.getId(), pagoPersistido.getMonto(), pagoPersistido.getSaldoRestante());

        return pagoPersistido;
    }

    private void copiarAtributosDetalle(DetallePago destino, DetallePago origen) {
        destino.setAlumno(origen.getAlumno());
        destino.setPago(origen.getPago());
        destino.setConcepto(origen.getConcepto());
        destino.setSubConcepto(origen.getSubConcepto());
        destino.setDescripcionConcepto(origen.getDescripcionConcepto());
        destino.setCuotaOCantidad(origen.getCuotaOCantidad());
        destino.setValorBase(origen.getValorBase());
        destino.setImporteInicial(origen.getImporteInicial());
        destino.setaCobrar(origen.getaCobrar());
        destino.setCobrado(origen.getCobrado());
        destino.setTipo(origen.getTipo());
        destino.setStock(origen.getStock());
        destino.setTieneRecargo(origen.getTieneRecargo());
        destino.setRecargo(origen.getRecargo());
        destino.setBonificacion(origen.getBonificacion());

        log.info("[copiarAtributosDetalle] Atributos copiados al nuevo detalle.");
    }

    private void actualizarDetalleDesdeRequest(DetallePago persistido, DetallePago request) {
        persistido.setDescripcionConcepto(request.getDescripcionConcepto());
        persistido.setaCobrar(request.getaCobrar());
        persistido.setImporteInicial(request.getImporteInicial());
        persistido.setValorBase(request.getValorBase());
        persistido.setTipo(request.getTipo());
        persistido.setTieneRecargo(request.getTieneRecargo());
        persistido.setFechaRegistro(LocalDate.now());
        persistido.setRecargo(request.getRecargo());
        persistido.setTieneRecargo(request.getTieneRecargo());
        if (!request.getTieneRecargo() && request.getMensualidad() != null || request.getTipo() == TipoDetallePago.MENSUALIDAD) {
            persistido.setImportePendiente(request.getImporteInicial());
        }
    }

    /**
     * Marca un pago como HISTORICO:
     * - Se fija su estado a HISTORICO.
     * - Se ajusta su saldo a 0.
     * - Se recorren sus DetallePago para:
     * - Marcar cada uno como 'cobrado'.
     * - Fijar su importe pendiente en 0.
     * Se persisten los cambios.
     */
    @Transactional
    protected void marcarPagoComoHistorico(Pago pago) {
        pago.setEstadoPago(EstadoPago.HISTORICO);
        pago.setSaldoRestante(0.0);
        for (DetallePago dp : pago.getDetallePagos()) {
            dp.setCobrado(true);
            dp.setImportePendiente(0.0);
            dp.setUsuario(pago.getUsuario());
            entityManager.merge(dp);
        }
        pago.setUsuario(pago.getUsuario());
        entityManager.merge(pago);
        entityManager.flush();
        log.info("[marcarPagoComoHistorico] Pago id={} marcado como HISTORICO", pago.getId());
    }


    /**
     * Asigna el metodo de pago al pago, recalcula totales y retorna el pago actualizado.
     */
    @Transactional
    protected void asignarMetodoYPersistir(Pago pago, Long metodoPagoId) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }
        log.info("[asignarMetodoYPersistir] Asignando método de pago para Pago id={}", pago.getId());

        // Buscar método de pago por id o usar 'EFECTIVO' por defecto.
        MetodoPago metodoPago = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseGet(() -> {
                    log.info("[asignarMetodoYPersistir] Método de pago con id={} no encontrado, asignando 'EFECTIVO'", metodoPagoId);
                    return metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO");
                });
        pago.setMetodoPago(metodoPago);

        // Persistir y forzar flush para obtener el ID asignado
        pagoRepositorio.saveAndFlush(pago);
        log.info("[asignarMetodoYPersistir] Pago persistido con ID: {}", pago.getId());

        // Si alguno de los detalles tiene recargo, se aplica el recargo del método de pago
        boolean aplicarRecargo = pago.getDetallePagos().stream()
                .anyMatch(DetallePago::getTieneRecargo);
        if (aplicarRecargo) {
            double recargo = (metodoPago.getRecargo() != null) ? metodoPago.getRecargo() : 0;
            log.info("[asignarMetodoYPersistir] Se aplicó recargo de {}. Nuevo monto: {}", recargo, pago.getMonto());
        }

        // Persistir nuevamente si es necesario y forzar el flush para actualizar el ID en el contexto de la transacción
        pagoRepositorio.saveAndFlush(pago);
    }

}
