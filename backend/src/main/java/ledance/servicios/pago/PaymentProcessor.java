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
 * - Se centraliza el calculo del abono y la actualizacion de importes en el metodo
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
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final RecargoRepositorio recargoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio,
                            DetallePagoServicio detallePagoServicio,
                            BonificacionRepositorio bonificacionRepositorio,
                            RecargoRepositorio recargoRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
    }

    // Método corregido para recalcular correctamente los totales
    public void recalcularTotales(Pago pago) {
        log.info("[recalcularTotales] Iniciando recalculo de totales para Pago ID: {}", pago.getId());

        BigDecimal montoTotalAbonado = BigDecimal.ZERO;
        BigDecimal saldoTotalPendiente = BigDecimal.ZERO;

        for (DetallePago detalle : pago.getDetallePagos()) {
            log.info("[recalcularTotales] Procesando DetallePago id={}", detalle.getId());
            // Asignamos siempre el alumno para asegurar consistencia
            detalle.setAlumno(pago.getAlumno());
            log.info("[recalcularTotales] Alumno asignado al detalle id={}", detalle.getId());

            // Aquí NO reiniciamos importePendiente, usamos el valor ya actualizado en el proceso de abono
            double abonoAplicado = (detalle.getaCobrar() != null) ? detalle.getaCobrar() : 0.0;
            log.info("[recalcularTotales] Detalle id={} - abono aplicado: {}", detalle.getId(), abonoAplicado);
            montoTotalAbonado = montoTotalAbonado.add(BigDecimal.valueOf(abonoAplicado));

            double importePendiente = (detalle.getImportePendiente() != null) ? detalle.getImportePendiente() : 0.0;
            log.info("[recalcularTotales] Detalle id={} - importe pendiente actual: {}", detalle.getId(), importePendiente);
            saldoTotalPendiente = saldoTotalPendiente.add(BigDecimal.valueOf(importePendiente));
        }

        double montoFinal = montoTotalAbonado.doubleValue();
        log.info("[recalcularTotales] Suma de abonos y pendientes: {} + {} = {}",
                montoTotalAbonado, saldoTotalPendiente, montoFinal);

        pago.setMonto(montoFinal);
        log.info("[recalcularTotales] Monto total asignado al pago: {}", pago.getMonto());

        pago.setMontoPagado(montoTotalAbonado.doubleValue());
        log.info("[recalcularTotales] Monto abonado asignado al pago: {}", pago.getMontoPagado());

        pago.setSaldoRestante(saldoTotalPendiente.doubleValue());
        log.info("[recalcularTotales] Saldo restante asignado al pago: {}", pago.getSaldoRestante());

        if (saldoTotalPendiente.compareTo(BigDecimal.ZERO) == 0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[recalcularTotales] Saldo pendiente es 0. Pago id={} marcado como HISTORICO", pago.getId());
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
            log.info("[recalcularTotales] Saldo pendiente es mayor que 0. Pago id={} marcado como ACTIVO", pago.getId());
        }
        log.info("[recalcularTotales] Recalculo finalizado para Pago ID: {}: Monto={}, Pagado={}, SaldoRestante={}",
                pago.getId(), pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante());
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
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

    /**
     * Verifica que el saldo restante no sea negativo. Si es negativo, lo ajusta a 0.
     */
    @Transactional
    void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.",
                    pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}",
                    pago.getId(), pago.getSaldoRestante());
        }
    }

    // 2. Determinar si es aplicable el pago historico, estandarizando la generacion de claves
    @Transactional
    public boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        log.info("[esPagoHistoricoAplicable] Iniciando verificación basada únicamente en el importe pendiente.");
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] No se puede aplicar pago histórico: pago o detallePagos es nulo.");
            return false;
        }

        List<DetallePago> detallesHistoricos = ultimoPendiente.getDetallePagos();
        List<DetallePagoRegistroRequest> detallesRequest = request.detallePagos();

        // Emparejar cada detalle del request por su descripción
        for (DetallePagoRegistroRequest reqDetalle : detallesRequest) {
            Optional<DetallePago> optHist = detallesHistoricos.stream()
                    .filter(hist -> hist.getDescripcionConcepto() != null &&
                            hist.getDescripcionConcepto().trim().equalsIgnoreCase(reqDetalle.descripcionConcepto().trim()))
                    .findFirst();
            if (optHist.isPresent()) {
                // Solo "limpiar" el recargo si en el request se indica explícitamente que no se debe aplicar (tieneRecargo == false)
                if (reqDetalle.tieneRecargo() != null && !reqDetalle.tieneRecargo()) {
                    log.info("[esPagoHistoricoAplicable] Para detalle '{}' se indica que NO se debe aplicar recargo.",
                            reqDetalle.descripcionConcepto());
                    DetallePago hist = optHist.get();
                    hist.setRecargo(null);
                    hist.setTieneRecargo(false);
                    // Recalcular el importe pendiente sin recargo (asumiendo que el importeInicial es el valor base)
                    hist.setImportePendiente(hist.getImporteInicial());
                } else {
                    log.info("[esPagoHistoricoAplicable] Para detalle '{}' se mantiene el recargo (tieneRecargo true).",
                            reqDetalle.descripcionConcepto());
                }
            } else {
                log.info("[esPagoHistoricoAplicable] No se encontró detalle histórico para '{}'.", reqDetalle.descripcionConcepto());
            }
        }

        // Calcular el total pendiente a partir de los detalles ya "limpiados" (cuando corresponde)
        double totalPendienteHistorico = ultimoPendiente.getDetallePagos().stream()
                .filter(detalle -> detalle.getImportePendiente() != null
                        && detalle.getImportePendiente() > 0.0
                        && !detalle.getCobrado())
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        log.info("[esPagoHistoricoAplicable] Total pendiente en pago histórico (después de limpiar recargos si corresponde): {}", totalPendienteHistorico);

        double totalAAbonarRequest = request.detallePagos().stream()
                .mapToDouble(dto -> dto.aCobrar() != null ? dto.aCobrar() : 0.0)
                .sum();
        log.info("[esPagoHistoricoAplicable] Total a abonar en request: {}", totalAAbonarRequest);

        boolean aplicable = totalPendienteHistorico >= totalAAbonarRequest;
        log.info("[esPagoHistoricoAplicable] Resultado: aplicable={}", aplicable);
        return aplicable;
    }

    @Transactional
    public Pago actualizarPagoHistoricoConAbonos(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago id={} con abonos", pagoHistorico.getId());

        // Iterar sobre cada detalle del request
        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            log.info("[actualizarPagoHistoricoConAbonos] Procesando detalle del request: '{}'", detalleReq.descripcionConcepto());
            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto() != null &&
                            d.getDescripcionConcepto().trim().equalsIgnoreCase(detalleReq.descripcionConcepto().trim()) &&
                            d.getImportePendiente() != null &&
                            d.getImportePendiente() > 0.0 &&
                            !d.getCobrado())
                    .findFirst();

            if (detalleOpt.isPresent()) {
                DetallePago detalleExistente = detalleOpt.get();
                log.info("[actualizarPagoHistoricoConAbonos] Detalle existente encontrado id={} para '{}'",
                        detalleExistente.getId(), detalleReq.descripcionConcepto());
                double nuevoACobrar = detalleReq.aCobrar();
                log.info("[actualizarPagoHistoricoConAbonos] Actualizando aCobrar del detalle id={} de {} a {}",
                        detalleExistente.getId(), detalleExistente.getaCobrar(), nuevoACobrar);
                detalleExistente.setaCobrar(nuevoACobrar);
                log.info("[actualizarPagoHistoricoConAbonos] Llamando a procesarDetalle para Detalle id={}", detalleExistente.getId());
                procesarDetalle(pagoHistorico, detalleExistente, pagoHistorico.getAlumno());
            } else {
                log.info("[actualizarPagoHistoricoConAbonos] No se encontró detalle existente para '{}'. Creando nuevo detalle.",
                        detalleReq.descripcionConcepto());
                DetallePago nuevoDetalle = crearNuevoDetalleFromRequest(detalleReq, pagoHistorico);
                log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle creado: '{}', importePendiente={}",
                        nuevoDetalle.getDescripcionConcepto(), nuevoDetalle.getImportePendiente());
                if (nuevoDetalle.getImportePendiente() > 0) {
                    log.info("[actualizarPagoHistoricoConAbonos] Agregando nuevo detalle al pago id={}", pagoHistorico.getId());
                    pagoHistorico.getDetallePagos().add(nuevoDetalle);
                    log.info("[actualizarPagoHistoricoConAbonos] Llamando a procesarDetalle para nuevo Detalle id={}", nuevoDetalle.getId());
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
        log.info("[procesarDetalle] INICIO. DetallePago id={} en Pago id={}", detalle.getId(), pago.getId());

        // Asignar alumno al detalle (ya debería estar asignado, pero se reasegura)
        log.info("[procesarDetalle] Valor de detalle.getAlumno() ANTES: {}",
                (detalle.getAlumno() != null ? detalle.getAlumno().getId() : "null"));
        detalle.setAlumno(alumnoPersistido);
        log.info("[procesarDetalle] Alumno asignado al detalle: ahora alumno={}",
                (detalle.getAlumno() != null ? detalle.getAlumno().getId() : "null"));

        // Si no tiene recargo, forzamos recargo null
        if (!Boolean.TRUE.equals(detalle.getTieneRecargo())) {
            detalle.setRecargo(null);
            log.info("[procesarDetalle] Se asigna recargo null al DetallePago id={} porque tieneRecargo es false", detalle.getId());
        }

        // Asegurar que el pago esté persistido
        if (pago.getId() == null) {
            log.info("[procesarDetalle] Pago no persistido. Persistiendo...");
            entityManager.persist(pago);
            entityManager.flush();
            log.info("[procesarDetalle] Pago persistido: ID={}", pago.getId());
        }
        // Forzar asociación del detalle con el pago si es necesario
        if (detalle.getPago() == null || detalle.getPago().getId() == null || detalle.getPago().getId() == 0) {
            detalle.setPago(pago);
            log.info("[procesarDetalle] Se asigna el pago id={} al detalle id={}", pago.getId(), detalle.getId());
        }

        // Reatachar asociaciones para asegurar que las entidades estén en estado managed
        paymentCalculationServicio.reatacharAsociaciones(detalle, pago);
        log.info("[procesarDetalle] Asociaciones reatachadas para DetallePago id={}", detalle.getId());

        // Obtener inscripción (si aplica) y loguear
        Inscripcion inscripcion = obtenerInscripcion(detalle);
        if (inscripcion != null) {
            log.info("[procesarDetalle] Inscripción encontrada para Detalle id={}: ID={}", detalle.getId(), inscripcion.getId());
        } else {
            log.info("[procesarDetalle] No se encontró inscripción para Detalle id={}", detalle.getId());
        }

        // Procesar y calcular el detalle
        paymentCalculationServicio.procesarYCalcularDetalle(pago, detalle, inscripcion);
        log.info("[procesarDetalle] FIN. Procesamiento y cálculo finalizado para DetallePago id={}", detalle.getId());
    }

    /**
     * Refactor de crearNuevoDetalleFromRequest:
     * - Se normaliza la descripción y se determinan las asociaciones.
     * - Se asignan el alumno, el pago y se configuran las propiedades base.
     * - Se invoca el cálculo de importes para establecer los valores finales.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] Iniciando creación de nuevo DetallePago a partir del request: {}", req);
        DetallePago detalle = new DetallePago();

        // Si el ID del request es 0, se asigna null para que se genere uno nuevo al persistir
        if (req.id() == 0) {
            detalle.setId(null);
            log.info("[crearNuevoDetalleFromRequest] ID del request era 0, se asigna null al nuevo DetallePago.");
        }

        // Asignar el alumno y el pago al detalle
        detalle.setAlumno(pago.getAlumno());
        detalle.setPago(pago);
        log.info("[crearNuevoDetalleFromRequest] Alumno y Pago asignados al DetallePago: {}", pago.getAlumno());

        // Normalizar y asignar la descripción (se utiliza mayúsculas para estandarizar)
        String descripcion = req.descripcionConcepto().trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[crearNuevoDetalleFromRequest] Descripción asignada y normalizada: '{}'", descripcion);

        // Asignar valor base y cantidad/couta
        detalle.setValorBase(req.valorBase());
        detalle.setCuotaOCantidad(req.cuotaOCantidad());
        log.info("[crearNuevoDetalleFromRequest] Valor base asignado: {}", req.valorBase());

        // Determinar y asignar el tipo del detalle según la descripción
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[crearNuevoDetalleFromRequest] Tipo determinado para DetallePago: {}", tipo);

        // Asignar bonificación si se indicó en el request
        if (req.bonificacionId() != null) {
            detalle.setBonificacion(obtenerBonificacionPorId(req.bonificacionId()));
            log.info("[crearNuevoDetalleFromRequest] Bonificación asignada con ID: {}", req.bonificacionId());
        }

        // Asignar recargo si corresponde (se verifica que tengaRecargo sea true y se haya indicado recargoId)
        if (Boolean.TRUE.equals(req.tieneRecargo()) && req.recargoId() != null) {
            detalle.setRecargo(obtenerRecargoPorId(req.recargoId()));
            log.info("[crearNuevoDetalleFromRequest] Recargo asignado con ID: {}", req.recargoId());
        } else {
            detalle.setRecargo(null);
            log.info("[crearNuevoDetalleFromRequest] No se asigna recargo (tieneRecargo es false o recargoId es nulo).");
        }

        // Calcular importes a partir de los valores base, aplicando descuentos, recargos, etc.
        detallePagoServicio.calcularImporte(detalle);
        log.info("[crearNuevoDetalleFromRequest] Se ha calculado el importe para DetallePago: importeInicial={}, importePendiente={}",
                detalle.getImporteInicial(), detalle.getImportePendiente());

        // Marcar el detalle como cobrado si el importe pendiente es 0 o menor
        detalle.setCobrado(detalle.getImportePendiente() <= 0.0);
        log.info("[crearNuevoDetalleFromRequest] DetallePago marcado como cobrado: {}", detalle.getCobrado());

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
        log.info("[clonarDetallesConPendiente] Iniciando clonación de detalles pendientes del pago histórico, ID: {}", pagoHistorico.getId());

        // Crear nuevo pago con datos básicos
        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        log.info("[clonarDetallesConPendiente] Asignado alumno: {}", nuevoPago.getAlumno());

        nuevoPago.setFecha(LocalDate.now());
        log.info("[clonarDetallesConPendiente] Asignada fecha actual: {}", nuevoPago.getFecha());

        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        log.info("[clonarDetallesConPendiente] Asignada fechaVencimiento: {}", nuevoPago.getFechaVencimiento());

        nuevoPago.setDetallePagos(new ArrayList<>());
        log.info("[clonarDetallesConPendiente] Inicializada lista vacía de detallePagos.");

        // Iterar sobre cada detalle del pago histórico
        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            log.info("[clonarDetallesConPendiente] Procesando DetallePago: ID={}, descripción='{}', importePendiente={}",
                    detalle.getId(), detalle.getDescripcionConcepto(), detalle.getImportePendiente());
            // Si el detalle no tiene recargo, se limpia esa asociación (según la lógica de negocio)
            if (!detalle.getTieneRecargo()) {
                detalle.setRecargo(null);
            }
            // Sólo clonar si tiene saldo pendiente (importePendiente > 0)
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0.0) {
                DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);
                nuevoPago.getDetallePagos().add(nuevoDetalle);
                log.info("[clonarDetallesConPendiente] Detalle clonado. ID antiguo: {}, importePendiente: {}",
                        detalle.getId(), detalle.getImportePendiente());
            } else {
                log.info("[clonarDetallesConPendiente] Detalle no clonado (saldado). ID: {}, importePendiente: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        // Si no se han clonado detalles pendientes, no se crea un nuevo pago
        if (nuevoPago.getDetallePagos().isEmpty()) {
            log.info("[clonarDetallesConPendiente] No hay detalles pendientes para clonar. No se crea nuevo pago.");
            return null;
        }

        // Recalcular totales del nuevo pago basado en los detalles clonados
        recalcularTotales(nuevoPago);
        log.info("[clonarDetallesConPendiente] Totales recalculados para nuevo pago.");

        double importeInicialCalculado = calcularImporteInicialDesdeDetalles(nuevoPago.getDetallePagos());
        nuevoPago.setImporteInicial(importeInicialCalculado);
        log.info("[clonarDetallesConPendiente] Importe inicial calculado para nuevo pago: {}", importeInicialCalculado);

        // Persistir el nuevo pago
        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] Nuevo pago creado con ID: {} y {} detalles pendientes",
                nuevoPago.getId(), nuevoPago.getDetallePagos().size());

        return nuevoPago;
    }

    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo") Double calcularImporteInicialDesdeDetalles(List<DetallePago> detallePagos) {
        log.info("[calcularImporteInicialDesdeDetalles] Iniciando calculo del importe inicial.");

        if (detallePagos == null || detallePagos.isEmpty()) {
            log.info("[calcularImporteInicialDesdeDetalles] Lista de DetallePagos nula o vacia. Retornando 0.0");
            return 0.0;
        }

        double total = detallePagos.stream()
                .filter(Objects::nonNull)
                .mapToDouble(detalle -> Optional.ofNullable(detalle.getImporteInicial()).orElse(0.0))
                .sum();

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
        log.info("[clonarDetallePago] Clonando DetallePago. Original ID: {}", original.getId());
        DetallePago clone = new DetallePago();
        log.info("[clonarDetallePago] Objeto DetallePago instanciado.");

        // Marcar este DetallePago como clon para futuras consultas
        clone.setEsClon(true);

        // Propiedades básicas y asociaciones
        log.info("[clonarDetallePago] Asignando descripcionConcepto.");
        clone.setDescripcionConcepto(original.getDescripcionConcepto());

        log.info("[clonarDetallePago] Asignando concepto.");
        clone.setConcepto(original.getConcepto());

        log.info("[clonarDetallePago] Asignando subConcepto.");
        clone.setSubConcepto(original.getSubConcepto());

        log.info("[clonarDetallePago] Asignando cuotaOCantidad.");
        clone.setCuotaOCantidad(original.getCuotaOCantidad());

        log.info("[clonarDetallePago] Asignando bonificacion.");
        clone.setBonificacion(original.getBonificacion());

        if (original.getTieneRecargo()) {
            log.info("[clonarDetallePago] Original tiene recargo, asignando recargo.");
            clone.setRecargo(original.getRecargo());
        }

        log.info("[clonarDetallePago] Asignando valorBase.");
        clone.setValorBase(original.getValorBase());

        log.info("[clonarDetallePago] Asignando tipo.");
        clone.setTipo(original.getTipo());

        log.info("[clonarDetallePago] Asignando fechaRegistro (actual).");
        clone.setFechaRegistro(LocalDate.now());

        // Calcular el importe pendiente a usar para la clonación
        log.info("[clonarDetallePago] Calculando importePendienteRestante.");
        double importePendienteRestante = original.getImportePendiente() != null
                ? original.getImportePendiente()
                : original.getImporteInicial();
        log.info("[clonarDetallePago] Importe pendiente restante calculado: {}", importePendienteRestante);

        log.info("[clonarDetallePago] Asignando importeInicial e importePendiente.");
        clone.setImporteInicial(importePendienteRestante);
        clone.setImportePendiente(importePendienteRestante);

        log.info("[clonarDetallePago] Asignando alumno desde nuevoPago.");
        clone.setAlumno(nuevoPago.getAlumno());

        clone.setMensualidad(original.getMensualidad());

        clone.setMatricula(original.getMatricula());

        log.info("[clonarDetallePago] Asignando stock.");
        clone.setStock(original.getStock());

        log.info("[clonarDetallePago] Asignando flag cobrado (true si importePendienteRestante es 0).");
        clone.setCobrado(importePendienteRestante == 0);

        log.info("[clonarDetallePago] Asignando nuevo pago.");
        clone.setPago(nuevoPago);

        log.info("[clonarDetallePago] Clonación completada, retornando clone.");
        return clone;
    }

    /**
     * Refactor de findDetallePagoByCriteria (se mantiene para compatibilidad, aunque la fusión se maneja con el mapa).
     * Busca un DetallePago basado en descripción, tipo y, si aplica, el id de matrícula o mensualidad.
     */
    public DetallePago findDetallePagoByCriteria(DetallePago detalle, Long alumnoId) {
        String descripcion = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto().trim().toUpperCase()
                : "";
        Long matriculaId = (detalle.getMatricula() != null) ? detalle.getMatricula().getId() : null;
        Long mensualidadId = (detalle.getMensualidad() != null) ? detalle.getMensualidad().getId() : null;
        TipoDetallePago tipo = detalle.getTipo();
        log.info("Buscando DetallePago para alumnoId={}, descripción='{}', tipo={}, matriculaId={}, mensualidadId={}",
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

    /**
     * Procesa los detalles de pago: asigna alumno y pago a cada DetallePago,
     * separa los detalles ya persistidos de los nuevos, reatacha las asociaciones y recalcula totales.
     */
    @Transactional
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumnoPersistido) {
        log.info("[processDetallesPago] INICIO. Pago inicial: ID={}, alumno.id={}",
                pago.getId(), (pago.getAlumno() != null ? pago.getAlumno().getId() : "null"));
        log.info("[processDetallesPago] Alumno persistido recibido: ID={}", alumnoPersistido.getId());

        // Aseguramos que el pago tenga el alumno asignado
        pago.setAlumno(alumnoPersistido);
        for (DetallePago dp : pago.getDetallePagos()) {
            dp.setAlumno(pago.getAlumno());
        }
        log.info("[processDetallesPago] Alumno asignado al pago: ID={}", alumnoPersistido.getId());

        // Si el pago no está persistido, lo persistimos primero
        if (pago.getId() == null || pago.getId() == 0) {
            log.info("[processDetallesPago] Pago no persistido. Persistiendo...");
            entityManager.persist(pago);
            entityManager.flush();
            log.info("[processDetallesPago] Pago persistido: ID={}", pago.getId());
        }
        Pago pagoFinal = pago;

        // Construir mapa de detalles ya persistidos (se agrupan por clave)
        Map<String, Queue<DetallePago>> detallesExistentes = new HashMap<>();
        if (pagoFinal.getDetallePagos() != null) {
            for (DetallePago dp : pagoFinal.getDetallePagos()) {
                if (dp.getId() != null && dp.getId() != 0) {
                    String clave = generarClaveDetalle(dp);
                    log.info("[processDetallesPago] Detalle persistido encontrado: ID={}, clave={}", dp.getId(), clave);
                    detallesExistentes.computeIfAbsent(clave, k -> new LinkedList<>()).add(dp);
                } else {
                    log.info("[processDetallesPago] Ignorando detalle con id nulo o 0: {}", dp.getId());
                }
            }
        } else {
            log.info("[processDetallesPago] No hay detalles existentes en el pago.");
        }

        List<DetallePago> detallesProcesados = new ArrayList<>();

        // Antes de reinicializar o procesar cada detalle, forzamos que cada uno reciba el alumno
        for (DetallePago detalleRequest : detallesFront) {
            log.info("[processDetallesPago] (PRE) Detalle recibido del request: descripcionConcepto='{}', id={}, alumno={}",
                    detalleRequest.getDescripcionConcepto(),
                    detalleRequest.getId(),
                    (detalleRequest.getAlumno() != null ? detalleRequest.getAlumno().getId() : "null"));

            // Asignamos el alumno desde el inicio
            detalleRequest.setAlumno(alumnoPersistido);
            log.info("[processDetallesPago] (POST) Alumno asignado al detalle: ahora alumno={}",
                    (detalleRequest.getAlumno() != null ? detalleRequest.getAlumno().getId() : "null"));
        }

        // Procesar cada detalle recibido
        for (DetallePago detalleRequest : detallesFront) {
            log.info("[processDetallesPago] Procesando Detalle: descripcionConcepto='{}', id={}",
                    detalleRequest.getDescripcionConcepto(), detalleRequest.getId());

            // Si el id es 0, reinicializamos id y version
            if (detalleRequest.getId() != null && detalleRequest.getId() == 0) {
                log.info("[processDetallesPago] Detalle con id=0 detectado. Reinicializando id y version. Valor alumno ANTES: {}",
                        (detalleRequest.getAlumno() != null ? detalleRequest.getAlumno().getId() : "null"));
                detalleRequest.setId(null);
                detalleRequest.setVersion(null);
                log.info("[processDetallesPago] (POST reinicialización) id={}, version={}, alumno={}",
                        detalleRequest.getId(), detalleRequest.getVersion(),
                        (detalleRequest.getAlumno() != null ? detalleRequest.getAlumno().getId() : "null"));
            }

            // Forzar asignación del pago (si aún no está asignado correctamente)
            if (detalleRequest.getPago() == null || detalleRequest.getPago().getId() == null || detalleRequest.getPago().getId() == 0) {
                detalleRequest.setPago(pagoFinal);
                log.info("[processDetallesPago] Se asigna el pago (ID={}) al detalle.", pagoFinal.getId());
            }
            // Volvemos a llamar a reatacharAsociaciones para reasegurar las demás asociaciones
            paymentCalculationServicio.reatacharAsociaciones(detalleRequest, pagoFinal);

            // Generar clave para buscar coincidencias en detalles existentes
            String clave = generarClaveDetalle(detalleRequest);
            log.info("[processDetallesPago] Clave generada para detalle: {}", clave);

            DetallePago detalleAProcesar;
            Queue<DetallePago> cola = detallesExistentes.get(clave);
            if (cola != null && !cola.isEmpty()) {
                detalleAProcesar = cola.poll();
                log.info("[processDetallesPago] Se encontró detalle persistido para clave '{}': ID={}", clave, detalleAProcesar.getId());
            } else {
                log.info("[processDetallesPago] No se encontró detalle persistido para clave '{}'. Se usará el detalle del request.", clave);
                // Reinicializamos id y version para indicar que es nuevo, pero conservamos alumno y pago asignados
                detalleRequest.setId(null);
                detalleRequest.setVersion(null);
                detalleAProcesar = detalleRequest;
            }

            // Procesar y calcular el detalle (internamente se asegura la persistencia del detalle)
            Inscripcion inscripcion = obtenerInscripcion(detalleAProcesar);
            log.info("[processDetallesPago] Inscripción obtenida para detalle (si aplica): {}",
                    (inscripcion != null ? inscripcion.getId() : "null"));
            procesarDetalle(pagoFinal, detalleAProcesar, alumnoPersistido);
            detallesProcesados.add(detalleAProcesar);
        }

        // Asignar la lista final de detalles al pago
        pagoFinal.getDetallePagos().clear();
        pagoFinal.getDetallePagos().addAll(detallesProcesados);
        log.info("[processDetallesPago] Total de detalles procesados: {}", detallesProcesados.size());

        // Recalcular totales del pago
        recalcularTotales(pagoFinal);
        log.info("[processDetallesPago] Totales recalculados: monto={}, saldoRestante={}",
                pagoFinal.getMonto(), pagoFinal.getSaldoRestante());

        // Persistir el pago final
        if (pagoFinal.getId() == null) {
            log.info("[processDetallesPago] Persistiendo pago final (nuevo).");
            entityManager.persist(pagoFinal);
            log.info("[processDetallesPago] Pago final persistido: ID={}", pagoFinal.getId());
        } else {
            log.info("[processDetallesPago] Realizando merge de pago final.");
            pagoFinal = entityManager.merge(pagoFinal);
            log.info("[processDetallesPago] Pago final mergeado: ID={}", pagoFinal.getId());
        }
        entityManager.flush();
        log.info("[processDetallesPago] FIN. Pago final persistido: ID={}, Monto={}, SaldoRestante={}",
                pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());
        return pagoFinal;
    }

    /**
     * Genera la clave de identificación para un DetallePago basándose en su descripción,
     * tipo y la asociación (por ejemplo, Matrícula o Mensualidad).
     */
    private String generarClaveDetalle(DetallePago detalle) {
        String desc = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto().trim().toUpperCase()
                : "";
        String tipo = (detalle.getTipo() != null)
                ? detalle.getTipo().toString()
                : "CONCEPTO";
        String idAsociacion = "";
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            idAsociacion = "MATRICULA:" + detalle.getMatricula().getId();
        } else if (detalle.getMensualidad() != null && detalle.getMensualidad().getId() != null) {
            idAsociacion = "MENSUALIDAD:" + detalle.getMensualidad().getId();
        }
        String clave = desc + "|" + tipo + "|" + idAsociacion;
        log.info("[generarClaveDetalle] Clave generada para DetallePago id={}: {}", detalle.getId(), clave);
        return clave;
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
            entityManager.merge(dp);
        }
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
        log.info("[asignarMetodoYPersistir] Método de pago asignado: {}", metodoPago);

        // Si alguno de los detalles tiene recargo, se aplica el recargo del método de pago
        boolean aplicarRecargo = pago.getDetallePagos().stream()
                .anyMatch(detalle -> Boolean.TRUE.equals(detalle.getTieneRecargo()));
        if (aplicarRecargo) {
            double recargo = (metodoPago.getRecargo() != null) ? metodoPago.getRecargo() : 0;
            pago.setMonto(pago.getMonto() + recargo);
            log.info("[asignarMetodoYPersistir] Se aplicó recargo de {}. Nuevo monto: {}", recargo, pago.getMonto());
        }

        // Persistir el pago (si no se persiste en otro lugar)
        pagoRepositorio.save(pago);
    }

}
