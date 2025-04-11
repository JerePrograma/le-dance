package ledance.servicios.pago;

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
    public void recalcularTotalesNuevo(Pago pagoNuevo) {
        log.info("[recalcularTotalesNuevo] Iniciando recalculo para nuevo pago (ID: {})", pagoNuevo.getId());

        // Inicialización de acumuladores.
        double totalACobrar = 0.0;
        Double totalPendiente = 0.0;

        // Procesa cada detalle del nuevo pago.
        for (DetallePago detalle : pagoNuevo.getDetallePagos()) {
            if (!detalle.getRemovido()) {
                log.info("[recalcularTotalesNuevo] Iniciando recalculo para detalle (ID: {}), : {})", detalle.getId(), detalle);
                detalle.setFechaRegistro(pagoNuevo.getFecha());
                double cobrado;
                if (detalle.getACobrar() == null || detalle.getACobrar() <= 0) {
                    cobrado = 0;
                } else {
                    cobrado = detalle.getACobrar();
                }
                totalACobrar += (cobrado);
                if (detalle.getImportePendiente() <= 0.0) {
                    detalle.setCobrado(true);
                    detalle.setImportePendiente(0.0);
                    detalle.setEstadoPago(EstadoPago.HISTORICO);
                    if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                        detalle.getMatricula().setPagada(true);
                    }
                }
                totalPendiente += detalle.getImportePendiente();
                log.info("[recalcularTotalesNuevo] Detalle ID {}: ACobrar={}, importePendiente={}. Acumulado: totalACobrar={}, totalPendiente={}",
                        detalle.getId(), cobrado, detalle.getImportePendiente(), totalACobrar, totalPendiente);
            }
        }

        // Determinar si se aplica recargo según el método de pago.
        double montoRecargo = 0.0;
        boolean aplicarRecargo = pagoNuevo.getDetallePagos().stream().anyMatch(DetallePago::getTieneRecargo);
        if (aplicarRecargo) {
            montoRecargo = pagoNuevo.getMetodoPago().getRecargo();
            log.info("[recalcularTotalesNuevo] Monto de recargo obtenido: {}", montoRecargo);
        } else {
            log.info("[recalcularTotalesNuevo] No se aplica recargo por método de pago.");
        }

        // Calcular el monto final sumando totalACobrar y el recargo.
        double montoFinal = totalACobrar + montoRecargo;
        log.info("[recalcularTotalesNuevo] Monto final previo al crédito: {}", montoFinal);

        // (Si se requiere aplicar saldo a favor en caso de matrícula, se puede incluir aquí esa lógica)

        // Asignar el monto final al pago nuevo.
        pagoNuevo.setMonto(montoFinal);
        pagoNuevo.setMontoPagado(montoFinal);

        // El saldo restante se asume igual a la suma de los importes pendientes (usualmente 0 si se abonan todos)
        double saldoRestante = totalPendiente;
        pagoNuevo.setSaldoRestante(saldoRestante);

        // Si el saldo restante es 0, se marca como HISTORICO; de lo contrario, se queda ACTIVO.
        if (totalPendiente <= 0) {
            pagoNuevo.setEstadoPago(EstadoPago.HISTORICO);
            pagoNuevo.setSaldoRestante(0.0);
        } else {
            pagoNuevo.setEstadoPago(EstadoPago.ACTIVO);
        }

        log.info("[recalcularTotalesNuevo] Finalizado para nuevo pago ID: {}. Monto={}, SaldoRestante={}, Estado={}",
                pagoNuevo.getId(), pagoNuevo.getMonto(), pagoNuevo.getSaldoRestante(), pagoNuevo.getEstadoPago());
    }

    /**
     * Obtiene la inscripcion asociada al detalle, si aplica.
     *
     * @param detalle el objeto DetallePago.
     * @return la Inscripcion asociada o null.
     */
    public Inscripcion obtenerInscripcion(DetallePago detalle) {
        log.info("[obtenerInscripcion] Buscando inscripción para DetallePago id={}", detalle.getId());
        if (detalle.getDescripcionConcepto().contains("CUOTA") && detalle.getMensualidad() != null &&
                detalle.getMensualidad().getInscripcion() != null) {
            return detalle.getMensualidad().getInscripcion();
        }
        return null;
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
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago histórico id={} con abonos", pagoHistorico.getId());

        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            String descripcionNormalizada = detalleReq.descripcionConcepto().trim().toUpperCase();

            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto() != null &&
                            d.getDescripcionConcepto().trim().equalsIgnoreCase(descripcionNormalizada) &&
                            d.getImportePendiente() != null &&
                            d.getImportePendiente() > 0.0 &&
                            !d.getCobrado())
                    .findFirst();

            if (detalleOpt.isPresent()) {
                DetallePago detalleExistente = detalleOpt.get();
                detalleExistente.setTieneRecargo(detalleReq.tieneRecargo());
                detalleExistente.setRemovido(detalleReq.removido());
                log.info("[actualizarPagoHistoricoConAbonos] Detalle existente encontrado ={}", detalleExistente);
                detalleExistente.setACobrar(detalleReq.ACobrar());
                detalleExistente.setImporteInicial(detalleReq.importePendiente());
                detalleExistente.setImportePendiente(detalleReq.importePendiente() - detalleReq.ACobrar());
                log.info("[actualizarPagoHistoricoConAbonos] Se actualiza importePendiente a {} en detalle id={}",
                        detalleReq.importePendiente(), detalleExistente.getId());
                procesarDetalle(pagoHistorico, detalleExistente, pagoHistorico.getAlumno());
                log.info("[actualizarPagoHistoricoConAbonos] Detalle procesado ={}", detalleExistente);
            } else {
                log.info("[actualizarPagoHistoricoConAbonos] No se encontró detalle existente para '{}'. Creando nuevo detalle.",
                        descripcionNormalizada);
                DetallePago nuevoDetalle = crearNuevoDetalleFromRequest(detalleReq, pagoHistorico);
                if (detalleReq.importePendiente() != null && detalleReq.importePendiente() > 0) {
                    nuevoDetalle.setImportePendiente(detalleReq.importePendiente());
                    nuevoDetalle.setImporteInicial(detalleReq.importePendiente());
                    log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle actualizado con importePendiente del frontend: {}",
                            detalleReq.importePendiente());
                }
                log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle creado: '{}', importePendiente={}",
                        nuevoDetalle.getDescripcionConcepto(), nuevoDetalle.getImportePendiente());
                if (nuevoDetalle.getImportePendiente() > 0) {
                    pagoHistorico.getDetallePagos().add(nuevoDetalle);
                    procesarDetalle(pagoHistorico, nuevoDetalle, pagoHistorico.getAlumno());
                } else {
                    log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle '{}' sin importe pendiente, se omite.",
                            nuevoDetalle.getDescripcionConcepto());
                }
            }
        }

        log.info("[actualizarPagoHistoricoConAbonos] Pago histórico id={} actualizado. Totales: monto={}, saldoRestante={}",
                pagoHistorico.getId(), pagoHistorico.getMonto(), pagoHistorico.getSaldoRestante());
        return pagoHistorico;
    }

    @Transactional
    public Pago clonarDetallesConPendiente(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[clonarDetallesConPendiente] INICIO - Clonando detalles pendientes para pago histórico ID: {}", pagoHistorico.getId());

        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        nuevoPago.setFecha(pagoHistorico.getFecha());
        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        nuevoPago.setDetallePagos(new ArrayList<>());
        nuevoPago.setMetodoPago(pagoHistorico.getMetodoPago());
        nuevoPago.setObservaciones(pagoHistorico.getObservaciones());

        int detallesClonados = 0;

        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            if (detalle.getImportePendiente() <= 0) {
                log.info("[clonarDetallesConPendiente] Detalle ID {} ya está pagado, se omite.", detalle.getId());
                continue;
            }
            log.info("[clonarDetallesConPendiente] Procesando detalle original ID: {}", detalle.getId());
            detalle.setFechaRegistro(pagoHistorico.getFecha());
            DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);
            nuevoDetalle.setFechaRegistro(nuevoPago.getFecha());

            // Si es de tipo CUOTA y tiene mensualidad, se marca la mensualidad como clon y se asigna.
            if (detalle.getDescripcionConcepto().contains("CUOTA") && detalle.getMensualidad() != null) {
                log.info("[clonarDetallesConPendiente] Marcando mensualidad como clon: {}", detalle.getMensualidad().getId());
                detalle.getMensualidad().setEsClon(true);
                nuevoDetalle.setMensualidad(detalle.getMensualidad());
            }

            // Buscar detalle en el request para asignar ACobrar si existe.
            Optional<DetallePagoRegistroRequest> detalleReqOpt = request.detallePagos().stream()
                    .filter(reqDetalle -> reqDetalle.descripcionConcepto().trim()
                            .equalsIgnoreCase(detalle.getDescripcionConcepto().trim()))
                    .findFirst();
            if (detalleReqOpt.isPresent()) {
                double ACobrar = (detalleReqOpt.get().ACobrar() == null || detalleReqOpt.get().ACobrar() <= 0)
                        ? 0 : detalleReqOpt.get().ACobrar();
                nuevoDetalle.setACobrar(ACobrar);
            } else {
                nuevoDetalle.setACobrar(detalle.getACobrar());
            }

            nuevoDetalle.setImporteInicial(detalle.getImporteInicial());
            nuevoDetalle.setImportePendiente(detalle.getImportePendiente());
            nuevoDetalle.setTipo(detalle.getTipo());
            if (nuevoDetalle.getConcepto() != null && nuevoDetalle.getSubConcepto() == null) {
                nuevoDetalle.setSubConcepto(nuevoDetalle.getConcepto().getSubConcepto());
            }

            nuevoPago.getDetallePagos().add(nuevoDetalle);
            detallesClonados++;
            log.info("[clonarDetallesConPendiente] Detalle clonado exitosamente. Total clonados: {}", detallesClonados);
        }

        if (detallesClonados == 0) {
            log.warn("[clonarDetallesConPendiente] No se clonaron detalles pendientes del histórico.");
            // Se deja la lista de detalles vacía para que la actualización posterior se encargue de crearlos según el request.
        }

        double importeInicial = calcularImporteInicialDesdeDetalles(nuevoPago.getDetallePagos());
        nuevoPago.setImporteInicial(importeInicial);
        nuevoPago.setMetodoPago(pagoHistorico.getMetodoPago());
        nuevoPago.setMonto(0.0);

        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] FIN - Nuevo pago creado con éxito. Detalles: {} | Nuevo ID: {}",
                nuevoPago.getDetallePagos().size(), nuevoPago.getId());
        return nuevoPago;
    }

    /**
     * Refactor de crearNuevoDetalleFromRequest:
     * - Se normaliza la descripción y se determinan las asociaciones.
     * - Se asignan el alumno, el pago y se configuran las propiedades base.
     * - Se invoca el cálculo de importes para establecer los valores finales.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] INICIO - Creando detalle desde request. Pago ID: {}", pago.getId());
        log.info("[crearNuevoDetalleFromRequest] Request recibido: {}", req.toString());

        DetallePago detalle = new DetallePago();

        // Manejo del ID
        log.info("[crearNuevoDetalleFromRequest] Procesando ID del request: {}", req.id());
        if (req.id() == 0) {
            log.info("[crearNuevoDetalleFromRequest] ID=0 recibido - Asignando null para generación automática");
            detalle.setId(null);
        } else {
            log.warn("[crearNuevoDetalleFromRequest] ID no cero recibido ({}) - Posible intento de modificación directa", req.id());
        }

        // Asignación de relaciones principales
        log.info("[crearNuevoDetalleFromRequest] Asignando alumno (ID: {}) y pago (ID: {})", pago.getAlumno().getId(), pago.getId());
        detalle.setAlumno(pago.getAlumno());
        detalle.setPago(pago);

        // Normalización de la descripción
        String descripcion = req.descripcionConcepto().trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[crearNuevoDetalleFromRequest] Descripción normalizada asignada: '{}'", detalle.getDescripcionConcepto());

        // Asignación de valores base (valor y cuota/cantidad)
        log.info("[crearNuevoDetalleFromRequest] Asignando valores base - Valor: {}, Cuota/Cantidad: {}", req.valorBase(), req.cuotaOCantidad());
        detalle.setValorBase(req.valorBase());
        detalle.setCuotaOCantidad(req.cuotaOCantidad());

        // Determinación del tipo de detalle a partir de la descripción
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);

        // Manejo de bonificación
        log.info("[crearNuevoDetalleFromRequest] Procesando bonificación - ID solicitado: {}", req.bonificacionId());
        if (req.bonificacionId() != null) {
            Bonificacion bonificacion = obtenerBonificacionPorId(req.bonificacionId());
            detalle.setBonificacion(bonificacion);
            log.info("[crearNuevoDetalleFromRequest] Bonificación asignada - ID: {}, Descripción: {}",
                    bonificacion.getId(), bonificacion.getDescripcion());
        }

        // Manejo de recargo
        log.info("[crearNuevoDetalleFromRequest] Procesando recargo - TieneRecargo: {}, RecargoID: {}", req.tieneRecargo(), req.recargoId());
        if (req.tieneRecargo()) {
            if (req.recargoId() != null) {
                Recargo recargo = obtenerRecargoPorId(req.recargoId());
                detalle.setRecargo(recargo);
                log.info("[crearNuevoDetalleFromRequest] Recargo asignado - ID: {}, Porcentaje: {}%",
                        recargo.getId(), recargo.getPorcentaje());
            } else {
                log.warn("[crearNuevoDetalleFromRequest] Flag tieneRecargo=true pero sin recargoId especificado");
            }
        } else {
            detalle.setTieneRecargo(false);
        }

        // Cálculo de importes mediante el servicio correspondiente
        log.info("[crearNuevoDetalleFromRequest] Invocando cálculo de importes");
        detallePagoServicio.calcularImporte(detalle);

        double ACobrar = (req.ACobrar() == null || req.ACobrar() == 0) ? 0 : req.ACobrar();
        detalle.setACobrar(ACobrar);

        if (req.importePendiente() != null) {
            log.info("[crearNuevoDetalleFromRequest] Forzando importePendiente desde request: {}", req.importePendiente());
            detalle.setImportePendiente(req.importePendiente());
        }

        // Determinar si el detalle se marca como cobrado
        boolean cobrado = detalle.getImportePendiente() <= 0.0;
        detalle.setCobrado(cobrado);

        log.info("[crearNuevoDetalleFromRequest] FIN - Detalle creado exitosamente. ID: {}, Tipo: {}, Cobrado: {}",
                (detalle.getId() != null ? detalle.getId() : "NUEVO"),
                detalle.getTipo(),
                detalle.getCobrado());
        log.info("[crearNuevoDetalleFromRequest] Detalle completo: {}", detalle);

        return detalle;
    }

    /**
     * Procesa un detalle individual: asigna alumno y pago, reatacha asociaciones y llama a la lógica
     * de procesamiento y cálculo de detalle.
     */
    private void procesarDetalle(Pago pago, DetallePago detalle, Alumno alumnoPersistido) {
        if (!detalle.getEsClon()) {
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
            log.info("[procesarDetalle] Alumno asignado verificado: {}", detalle.getAlumno().getId());

            log.info("[procesarDetalle] Asignando pago (id={}) al detalle", pago.getId());
            detalle.setPago(pago);
            log.info("[procesarDetalle] Pago asignado verificado: {}", detalle.getPago().getId());

            // 2. Ajuste de recargo
            log.info("[procesarDetalle] Verificando recargo. TieneRecargo={}", detalle.getTieneRecargo());
            if (!detalle.getTieneRecargo()) {
                log.info("[procesarDetalle] Sin recargo - Estableciendo recargo=null");
                detalle.setTieneRecargo(false);
                log.info("[procesarDetalle] Recargo verificado: {}", detalle.getRecargo());
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
            log.info("[procesarDetalle] Detalle procesado - Estado inicial: Cobrado={}, ACobrar={}, Pendiente={}",
                    detalle.getCobrado(),
                    detalle.getACobrar(),
                    detalle.getImportePendiente());
            // 6. Procesamiento principal
            log.info("[procesarDetalle] Invocando procesarYCalcularDetalle para detalle id={}", detalle.getId());
            paymentCalculationServicio.procesarYCalcularDetalle(detalle, inscripcion);
            log.info("[procesarDetalle] Detalle procesado - Estado final: Cobrado={}, ACobrar={}, Pendiente={}",
                    detalle.getCobrado(),
                    detalle.getACobrar(),
                    detalle.getImportePendiente());

            log.info("[procesarDetalle] FIN - Procesamiento completado para DetallePago id={} (Pago id={})",
                    detalle.getId(),
                    pago.getId());
            log.info("[procesarDetalle] Finalizando cálculo DetallePago: {}", detalle);
        }
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

    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo") Double calcularImporteInicialDesdeDetalles
            (List<DetallePago> detallePagos) {
        log.info("[calcularImporteInicialDesdeDetalles] Iniciando calculo del importe inicial.");

        if (detallePagos == null || detallePagos.isEmpty()) {
            log.info("[calcularImporteInicialDesdeDetalles] Lista de DetallePagos nula o vacia. Retornando 0.0");
            return 0.0;
        }

        double total = detallePagos.stream().filter(Objects::nonNull).mapToDouble(detalle -> Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0)).sum();

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
        log.info("[clonarDetallePago] Iniciando clonación de DetallePago {}", original);

        // Creación del clon
        log.info("[clonarDetallePago] Creando nueva instancia de DetallePago para el clon");
        DetallePago clone = new DetallePago();

        // Marcado de clon/original
        log.info("[clonarDetallePago] Configurando flags de clonación: clone.esClon=true, original.esClon=false");
        clone.setEsClon(true);
        original.setEsClon(false);

        // Copia de atributos básicos
        log.info("[clonarDetallePago] Copiando descripciónConcepto: {}", original.getDescripcionConcepto());
        clone.setDescripcionConcepto(original.getDescripcionConcepto());

        // Manejo de Concepto y SubConcepto
        log.info("[clonarDetallePago] Procesando Concepto y SubConcepto...");
        if (original.getConcepto() != null && original.getConcepto().getId() != null) {
            log.info("[clonarDetallePago] Concepto original tiene ID: {}", original.getConcepto().getId());
            Concepto managedConcepto = entityManager.find(Concepto.class, original.getConcepto().getId());

            if (managedConcepto != null) {
                log.info("[clonarDetallePago] Concepto encontrado en EntityManager, reattachando...");
                clone.setConcepto(managedConcepto);
                log.info("[clonarDetallePago] Concepto reatachado en el clon: {}", managedConcepto.getId());

                if (original.getSubConcepto() != null) {
                    log.info("[clonarDetallePago] SubConcepto original presente, copiando...");
                    clone.setSubConcepto(managedConcepto.getSubConcepto());
                    log.info("[clonarDetallePago] SubConcepto reatachado en el clon: {}", managedConcepto.getSubConcepto().getId());
                } else {
                    log.info("[clonarDetallePago] No hay SubConcepto en el original");
                }
            } else {
                log.warn("[clonarDetallePago] Concepto no encontrado en EntityManager, copiando referencia directa");
                clone.setConcepto(original.getConcepto());
                clone.setSubConcepto(original.getSubConcepto());
            }
        } else {
            log.info("[clonarDetallePago] Concepto original es null o sin ID, copiando referencia directa");
            clone.setConcepto(original.getConcepto());
            clone.setSubConcepto(original.getSubConcepto());
        }

        // Copia de atributos numéricos y booleanos
        log.info("[clonarDetallePago] Copiando cuotaOCantidad: {}", original.getCuotaOCantidad());
        clone.setCuotaOCantidad(original.getCuotaOCantidad());

        log.info("[clonarDetallePago] Copiando bonificacion: {}", original.getBonificacion());
        clone.setBonificacion(original.getBonificacion());

        // Manejo de recargo
        if (original.getTieneRecargo()) {
            log.info("[clonarDetallePago] Original tiene recargo: {}, copiando...", original.getRecargo());
            clone.setRecargo(original.getRecargo());
        }
        clone.setTieneRecargo(original.getTieneRecargo());

        log.info("[clonarDetallePago] Copiando valorBase: {}", original.getValorBase());
        clone.setValorBase(original.getValorBase());

        log.info("[clonarDetallePago] Copiando tipo: {}", original.getTipo());
        clone.setTipo(original.getTipo());

        log.info("[clonarDetallePago] Estableciendo fechaRegistro a hoy");
        clone.setFechaRegistro(LocalDate.now());

        log.info("[clonarDetallePago] importePendienteRestante calculado: {}", original.getImportePendiente());

        log.info("[clonarDetallePago] Configurando importeInicial: {}", original.getImportePendiente());
        clone.setImporteInicial(original.getImportePendiente());

        log.info("[clonarDetallePago] Configurando importePendiente: {}", original.getImportePendiente());
        clone.setImportePendiente(original.getImportePendiente());

        log.info("[clonarDetallePago] Asignando alumno del nuevo pago");
        clone.setAlumno(nuevoPago.getAlumno());

        log.info("[clonarDetallePago] Copiando ACobrar: {}", original.getACobrar());
        clone.setACobrar(original.getACobrar());

        // Manejo de Mensualidad
        log.info("[clonarDetallePago] Procesando mensualidad...");
        Mensualidad originalMensualidad = original.getMensualidad();
        if (originalMensualidad != null) {
            log.info("[clonarDetallePago] Mensualidad presente en original, marcando como clon y copiando");
            originalMensualidad.setEsClon(true);
            originalMensualidad.setDescripcion(originalMensualidad.getDescripcion());
            clone.setMensualidad(originalMensualidad);
        } else {
            log.info("[clonarDetallePago] No hay mensualidad en el original");
            clone.setMensualidad(null);
        }

        // Copia de atributos restantes
        log.info("[clonarDetallePago] Copiando matricula");
        clone.setMatricula(original.getMatricula());

        log.info("[clonarDetallePago] Copiando stock: {}", original.getStock());
        clone.setStock(original.getStock());

        boolean cobrado = original.getImportePendiente() == 0;
        log.info("[clonarDetallePago] Configurando cobrado: {}", cobrado);
        clone.setCobrado(cobrado);

        log.info("[clonarDetallePago] Asignando nuevo pago al clon");
        clone.setPago(nuevoPago);

        log.info("[clonarDetallePago] Clonación completada exitosamente para DetallePago original ID: {}", original.getId());
        return clone;
    }

    /**
     * Procesa los detalles de pago: asigna alumno y pago a cada DetallePago,
     * separa los detalles ya persistidos de los nuevos, reatacha las asociaciones y recalcula totales.
     */
    @Transactional
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumnoPersistido) {
        log.info("[processDetallesPago] INICIO. Pago ID={}, Alumno ID={}", pago.getId(), alumnoPersistido.getId());

        // Asignar alumno y limpiar o inicializar la lista de detalles
        pago.setAlumno(alumnoPersistido);
        if (pago.getDetallePagos() == null) {
            pago.setDetallePagos(new ArrayList<>());
            log.info("[processDetallesPago] detallePagos inicializado como nueva lista.");
        } else {
            pago.getDetallePagos().clear();
            log.info("[processDetallesPago] detallePagos limpiado.");
        }

        List<DetallePago> detallesProcesados = new ArrayList<>();

        // Procesamiento de cada detalle recibido desde el frontend
        for (DetallePago detalleRequest : detallesFront) {
            // Asignar asociaciones básicas
            detalleRequest.setAlumno(alumnoPersistido);
            detalleRequest.setPago(pago);
            detalleRequest.setUsuario(pago.getUsuario());

            // Se intenta buscar un detalle existente si el request trae ID
            DetallePago detallePersistido = null;
            if (detalleRequest.getId() != null && detalleRequest.getId() > 0) {
                detallePersistido = detallePagoRepositorio.findById(detalleRequest.getId()).orElse(null);
                log.info("[processDetallesPago] Buscando detallePersistido con ID={}: Encontrado={}",
                        detalleRequest.getId(), detallePersistido != null);
            }

            // Forzar la bandera de recargo
            if (!detalleRequest.getTieneRecargo()) {
                detalleRequest.setTieneRecargo(false);
            }

            // Si se encontró un detalle existente, se actualiza; de lo contrario se crea uno nuevo
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

        // Agregar todos los detalles procesados al pago
        pago.getDetallePagos().addAll(detallesProcesados);

        // Persistir el pago (nuevo o actualizado)
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

        // Para pagos nuevos, usamos el método de recálculo específico para nuevos pagos
        recalcularTotalesNuevo(pagoPersistido);

        log.info("[processDetallesPago] FIN. Pago ID={}, Monto={}, SaldoRestante={}",
                pagoPersistido.getId(), pagoPersistido.getMonto(), pagoPersistido.getSaldoRestante());

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
        destino.setACobrar(origen.getACobrar());
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
        persistido.setACobrar(request.getACobrar());
        persistido.setImporteInicial(request.getImporteInicial());
        // Si el request trae un importe pendiente, lo actualizamos;
        // de lo contrario, podrías asignar un valor basado en la lógica de negocio.
        if (request.getImportePendiente() != null) {
            persistido.setImportePendiente(request.getImportePendiente() - persistido.getACobrar());
        }
        persistido.setValorBase(request.getValorBase());
        persistido.setTipo(request.getTipo());
        persistido.setTieneRecargo(request.getTieneRecargo());
        persistido.setFechaRegistro(LocalDate.now());
        persistido.setRecargo(request.getRecargo());
        persistido.setTieneRecargo(request.getTieneRecargo());
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
    protected void cerrarPagoHistorico(Pago pago) {
        log.info("[cerrarPagoHistorico] Iniciando cierre para pago histórico ID: {}", pago.getId());

        // Actualizar el estado del pago y su saldo
        pago.setEstadoPago(EstadoPago.HISTORICO);
        pago.setSaldoRestante(0.0);

        // Iterar cada detalle para marcarlo como pagado
        for (DetallePago dp : pago.getDetallePagos()) {
            dp.setFechaRegistro(pago.getFecha());
            dp.setCobrado(true);
            dp.setImportePendiente(0.0);
            dp.setUsuario(pago.getUsuario());
            dp.setEstadoPago(EstadoPago.HISTORICO);
            // Se hace merge para que el EntityManager gestione los cambios
            entityManager.merge(dp);
        }

        // Merge del propio pago y flush para persistir inmediatamente los cambios
        entityManager.merge(pago);
        entityManager.flush();

        log.info("[cerrarPagoHistorico] Pago id={} marcado como HISTORICO", pago.getId());
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


    /**
     * Procesa el abono parcial:
     * 1. Actualiza el pago activo con los abonos.
     * 2. Clona los detalles pendientes en un nuevo pago.
     * 3. Marca el pago activo como HISTORICO.
     * Retorna el nuevo pago (con los detalles pendientes) que representa el abono en curso.
     */
    @Transactional
    public Pago procesarAbonoParcial(Pago pagoActivo, PagoRegistroRequest request) {
        log.info("[procesarAbonoParcial] INICIO - Procesando abono parcial para Pago ID: {}", pagoActivo.getId());

        // Clonar los detalles pendientes (si existen) del pago histórico.
        Pago nuevoPago = clonarDetallesConPendiente(pagoActivo, request);

        // Actualizar (o crear) los detalles a partir del request.
        Pago pagoHistoricoActualizado = actualizarPagoHistoricoConAbonos(nuevoPago, request);

        // Obtener el método de pago; si no se encuentra, se toma "EFECTIVO".
        MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                .orElseGet(() -> metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO"));
        nuevoPago.setMetodoPago(metodoPago);
        recalcularTotalesNuevo(nuevoPago);

        log.info("[procesarAbonoParcial] Pago histórico actualizado - ID: {}, Estado: {}, Monto: {}, Saldo: {}",
                pagoHistoricoActualizado.getId(),
                pagoHistoricoActualizado.getEstadoPago(),
                pagoHistoricoActualizado.getMonto(),
                pagoHistoricoActualizado.getSaldoRestante());

        // Asignar al pago histórico el cobrador y actualizar el método de pago.
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findById(request.usuarioId());
        Usuario cobrador = usuarioOpt.get();
        pagoHistoricoActualizado.setUsuario(cobrador);

        Optional<MetodoPago> metodoPagoOpt = metodoPagoRepositorio.findById(request.metodoPagoId());
        pagoHistoricoActualizado.setMetodoPago(metodoPagoOpt.get());

        // Cerrar el pago histórico original y guardar los cambios.
        cerrarPagoHistorico(pagoActivo);
        pagoHistoricoActualizado = pagoRepositorio.save(pagoHistoricoActualizado);

        log.info("[procesarAbonoParcial] Nuevo pago generado - ID: {}, Detalles: {}",
                nuevoPago.getId(), nuevoPago.getDetallePagos().size());
        nuevoPago.setMetodoPago(metodoPagoOpt.get());
        log.info("[procesarAbonoParcial] Pago histórico ahora es {} - ID: {}",
                pagoHistoricoActualizado.getEstadoPago(), pagoHistoricoActualizado.getId());

        pagoActivo.setObservaciones(request.observaciones());
        nuevoPago.setUsuario(cobrador);
        log.info("[procesarAbonoParcial] Retornando nuevo pago con detalles pendientes - ID: {}, Estado: {}",
                nuevoPago.getId(), nuevoPago.getEstadoPago());
        return nuevoPago;
    }

}
