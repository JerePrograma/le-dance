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

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Refactor del servicio PaymentProcessor.
 * Se han centralizado las operaciones clave:
 * - Recalcular totales en nuevo pago.
 * - Procesar abonos parciales, clonación de detalles pendientes y cierre del pago histórico.
 * - Procesar y calcular cada DetallePago de forma unificada.
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
    private final PagoRepositorio pagoRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            DetallePagoServicio detallePagoServicio,
                            BonificacionRepositorio bonificacionRepositorio,
                            RecargoRepositorio recargoRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio,
                            UsuarioRepositorio usuarioRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /**
     * Recalcula totales para un nuevo pago a partir de sus detalles.
     * Suma los importes a cobrar y pendientes, ajusta recargos y define estado.
     */
    @Transactional
    public void recalcularTotalesNuevo(Pago pagoNuevo) {
        log.info("[recalcularTotalesNuevo] Iniciando recalculo para nuevo pago (ID: {})", pagoNuevo.getId());
        double totalACobrar = 0.0;
        double totalPendiente = 0.0;
        for (DetallePago detalle : pagoNuevo.getDetallePagos()) {
            if (!detalle.getRemovido()) {
                detalle.setFechaRegistro(pagoNuevo.getFecha());
                // Usamos "aCobrar" (ya renombrado) en lugar de "ACobrar"
                double cobrado = (detalle.getACobrar() != null && detalle.getACobrar() > 0) ? detalle.getACobrar() : 0;
                totalACobrar += cobrado;
                if (detalle.getImportePendiente() <= 0.0) {
                    detalle.setCobrado(true);
                    detalle.setImportePendiente(0.0);
                    detalle.setEstadoPago(EstadoPago.HISTORICO);
                    if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                        detalle.getMatricula().setPagada(true);
                    }
                }
                totalPendiente += detalle.getImportePendiente() == null ? 0.0 : detalle.getImportePendiente();
                log.info("[recalcularTotalesNuevo] Detalle ID {}: aCobrar={}, importePendiente={}. Acumulado: totalACobrar={}, totalPendiente={}",
                        detalle.getId(), cobrado, detalle.getImportePendiente(), totalACobrar, totalPendiente);
            }
        }
        double montoRecargo = 0.0;
        if (pagoNuevo.getDetallePagos().stream().anyMatch(DetallePago::getTieneRecargo)) {
            montoRecargo = pagoNuevo.getMetodoPago().getRecargo() != null ? pagoNuevo.getMetodoPago().getRecargo() : 0;
        }
        double montoFinal = totalACobrar + montoRecargo;
        pagoNuevo.setMonto(montoFinal);
        pagoNuevo.setMontoPagado(montoFinal);
        pagoNuevo.setSaldoRestante(totalPendiente);
        pagoNuevo.setEstadoPago(totalPendiente <= 0 ? EstadoPago.HISTORICO : EstadoPago.ACTIVO);
        if (totalPendiente <= 0) {
            pagoNuevo.setSaldoRestante(0.0);
        }
        log.info("[recalcularTotalesNuevo] Finalizado para pago ID: {}. Monto={}, SaldoRestante={}, Estado={}",
                pagoNuevo.getId(), pagoNuevo.getMonto(), pagoNuevo.getSaldoRestante(), pagoNuevo.getEstadoPago());
    }

    /**
     * Actualiza un pago histórico con abonos: para cada detalle, actualiza el importe pendiente según el abono y
     * procesa el detalle (o actualiza uno existente). Se usa para integrar abonos en pagos históricos.
     */
    @Transactional
    public Pago actualizarPagoHistoricoConAbonos(Pago pagoHistorico, PagoRegistroRequest request) {
        log.info("[actualizarPagoHistoricoConAbonos] Iniciando actualización del pago histórico id={} con abonos", pagoHistorico.getId());
        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            String descripcionNorm = detalleReq.descripcionConcepto().trim().toUpperCase();
            Optional<DetallePago> detalleOpt = pagoHistorico.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto() != null &&
                            d.getDescripcionConcepto().trim().equalsIgnoreCase(descripcionNorm) &&
                            d.getImportePendiente() != null &&
                            d.getImportePendiente() > 0.0 &&
                            !d.getCobrado())
                    .findFirst();
            if (detalleOpt.isPresent()) {
                DetallePago detalleExistente = detalleOpt.get();
                detalleExistente.setTieneRecargo(detalleReq.tieneRecargo());
                detalleExistente.setRemovido(detalleReq.removido());
                detalleExistente.setACobrar(detalleReq.ACobrar());
                detalleExistente.setImporteInicial(detalleReq.importePendiente());
                detalleExistente.setImportePendiente(detalleReq.importePendiente() - detalleReq.ACobrar());
                procesarDetalle(pagoHistorico, detalleExistente, pagoHistorico.getAlumno());
                log.info("[actualizarPagoHistoricoConAbonos] Detalle actualizado id={}", detalleExistente.getId());
            } else {
                log.info("[actualizarPagoHistoricoConAbonos] No se encontró detalle para '{}'. Creando nuevo detalle.",
                        descripcionNorm);
                DetallePago nuevoDetalle = crearNuevoDetalleFromRequest(detalleReq, pagoHistorico);
                if (detalleReq.importePendiente() != null && detalleReq.importePendiente() > 0) {
                    nuevoDetalle.setImportePendiente(detalleReq.importePendiente());
                    nuevoDetalle.setImporteInicial(detalleReq.importePendiente());
                }
                if (nuevoDetalle.getImportePendiente() > 0) {
                    pagoHistorico.getDetallePagos().add(nuevoDetalle);
                    procesarDetalle(pagoHistorico, nuevoDetalle, pagoHistorico.getAlumno());
                }
            }
        }
        log.info("[actualizarPagoHistoricoConAbonos] Pago histórico id={} actualizado. Totales: monto={}, saldoRestante={}",
                pagoHistorico.getId(), pagoHistorico.getMonto(), pagoHistorico.getSaldoRestante());
        return pagoHistorico;
    }

    /**
     * Clona en un nuevo pago los DetallePago pendientes del pago histórico.
     */
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
            DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);
            nuevoDetalle.setFechaRegistro(nuevoPago.getFecha());
            // Si el detalle corresponde a una cuota, se marca la mensualidad como clon.
            if (detalle.getDescripcionConcepto().contains("CUOTA") && detalle.getMensualidad() != null) {
                detalle.getMensualidad().setEsClon(true);
                nuevoDetalle.setMensualidad(detalle.getMensualidad());
            }
            // Se puede ajustar aCobrar según el request si se encuentra en él.
            Optional<DetallePagoRegistroRequest> detalleReqOpt = request.detallePagos().stream()
                    .filter(reqDetalle -> reqDetalle.descripcionConcepto().trim().equalsIgnoreCase(detalle.getDescripcionConcepto().trim()))
                    .findFirst();
            nuevoDetalle.setACobrar(detalleReqOpt.map(req -> req.ACobrar()).orElse(detalle.getACobrar()));
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
     * Crea un DetallePago nuevo a partir de un DetallePagoRegistroRequest.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] INICIO - Creando detalle desde request. Pago ID: {}", pago.getId());
        DetallePago detalle = new DetallePago();
        // Se ignora el ID si es 0 para forzar la generación automática
        if (req.id() == 0) {
            detalle.setId(null);
        }
        detalle.setAlumno(pago.getAlumno());
        detalle.setPago(pago);
        String descripcion = req.descripcionConcepto().trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        detalle.setValorBase(req.valorBase());
        detalle.setCuotaOCantidad(req.cuotaOCantidad());
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        if (req.bonificacionId() != null) {
            Bonificacion bonificacion = obtenerBonificacionPorId(req.bonificacionId());
            detalle.setBonificacion(bonificacion);
        }
        if (req.tieneRecargo()) {
            if (req.recargoId() != null) {
                Recargo recargo = obtenerRecargoPorId(req.recargoId());
                detalle.setRecargo(recargo);
            } else {
                log.warn("[crearNuevoDetalleFromRequest] tieneRecargo es true pero no se proporcionó recargoId");
            }
        } else {
            detalle.setTieneRecargo(false);
        }
        detallePagoServicio.calcularImporte(detalle);
        double aCobrar = (req.ACobrar() != null && req.ACobrar() > 0) ? req.ACobrar() : 0;
        detalle.setACobrar(aCobrar);
        if (req.importePendiente() != null) {
            detalle.setImportePendiente(req.importePendiente());
        }
        detalle.setCobrado(detalle.getImportePendiente() <= 0.0);
        log.info("[crearNuevoDetalleFromRequest] FIN - Detalle creado exitosamente. Tipo: {}, Cobrado: {}",
                detalle.getTipo(), detalle.getCobrado());
        return detalle;
    }

    /**
     * Procesa un DetallePago individual: reatacha asociaciones, persiste pago si es nuevo,
     * y llama a la lógica central de cálculo.
     */
    private void procesarDetalle(Pago pago, DetallePago detalle, Alumno alumnoPersistido) {
        if (!detalle.getEsClon()) {
            detalle.setAlumno(alumnoPersistido);
            detalle.setPago(pago);
            if (!detalle.getTieneRecargo()) {
                detalle.setRecargo(null);
            }
            if (pago.getId() == null) {
                entityManager.persist(pago);
                entityManager.flush();
            }
            paymentCalculationServicio.reatacharAsociaciones(detalle, pago);
            Inscripcion inscripcion = obtenerInscripcion(detalle);
            paymentCalculationServicio.procesarYCalcularDetalle(detalle, inscripcion);
            log.info("[procesarDetalle] Procesamiento completado para DetallePago id={} (Pago id={})", detalle.getId(), pago.getId());
        }
    }

    private Bonificacion obtenerBonificacionPorId(Long id) {
        return bonificacionRepositorio.findById(id).orElse(null);
    }

    private Recargo obtenerRecargoPorId(Long id) {
        return recargoRepositorio.findById(id).orElse(null);
    }

    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo") Double calcularImporteInicialDesdeDetalles(List<DetallePago> detallePagos) {
        if (detallePagos == null || detallePagos.isEmpty()) {
            return 0.0;
        }
        double total = detallePagos.stream()
                .filter(Objects::nonNull)
                .mapToDouble(detalle -> Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0))
                .sum();
        return Math.max(0.0, total);
    }

    /**
     * Clona un DetallePago para asignárselo a un nuevo Pago.
     */
    private DetallePago clonarDetallePago(DetallePago original, Pago nuevoPago) {
        DetallePago clone = new DetallePago();
        clone.setEsClon(true);
        original.setEsClon(false);
        clone.setDescripcionConcepto(original.getDescripcionConcepto());
        if (original.getConcepto() != null && original.getConcepto().getId() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, original.getConcepto().getId());
            if (managedConcepto != null) {
                clone.setConcepto(managedConcepto);
                clone.setSubConcepto(managedConcepto.getSubConcepto());
            } else {
                clone.setConcepto(original.getConcepto());
                clone.setSubConcepto(original.getSubConcepto());
            }
        } else {
            clone.setConcepto(original.getConcepto());
            clone.setSubConcepto(original.getSubConcepto());
        }
        clone.setCuotaOCantidad(original.getCuotaOCantidad());
        clone.setBonificacion(original.getBonificacion());
        if (original.getTieneRecargo()) {
            clone.setRecargo(original.getRecargo());
        }
        clone.setTieneRecargo(original.getTieneRecargo());
        clone.setValorBase(original.getValorBase());
        clone.setTipo(original.getTipo());
        clone.setFechaRegistro(LocalDate.now());
        clone.setImporteInicial(original.getImportePendiente());
        clone.setImportePendiente(original.getImportePendiente());
        clone.setAlumno(nuevoPago.getAlumno());
        clone.setACobrar(original.getACobrar());
        clone.setMatricula(original.getMatricula());
        clone.setStock(original.getStock());
        clone.setCobrado(original.getImportePendiente() == 0);
        clone.setPago(nuevoPago);
        return clone;
    }

    /**
     * Procesa los detalles de pago recibidos (del frontend) y los asocia al pago.
     */
    @Transactional
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumnoPersistido) {
        pago.setAlumno(alumnoPersistido);
        if (pago.getDetallePagos() == null) {
            pago.setDetallePagos(new ArrayList<>());
        } else {
            pago.getDetallePagos().clear();
        }
        List<DetallePago> detallesProcesados = new ArrayList<>();
        for (DetallePago detalleRequest : detallesFront) {
            detalleRequest.setAlumno(alumnoPersistido);
            detalleRequest.setPago(pago);
            detalleRequest.setUsuario(pago.getUsuario());
            DetallePago detallePersistido = null;
            if (detalleRequest.getId() != null && detalleRequest.getId() > 0) {
                detallePersistido = detallePagoRepositorio.findById(detalleRequest.getId()).orElse(null);
            }
            if (detallePersistido != null) {
                detallePersistido.setUsuario(pago.getUsuario());
                actualizarDetalleDesdeRequest(detallePersistido, detalleRequest);
                procesarDetalle(pago, detallePersistido, alumnoPersistido);
                detallesProcesados.add(detallePersistido);
            } else {
                DetallePago nuevoDetalle = new DetallePago();
                copiarAtributosDetalle(nuevoDetalle, detalleRequest);
                nuevoDetalle.setFechaRegistro(LocalDate.now());
                TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(nuevoDetalle.getDescripcionConcepto());
                nuevoDetalle.setTipo(tipo);
                procesarDetalle(pago, nuevoDetalle, alumnoPersistido);
                nuevoDetalle.setUsuario(pago.getUsuario());
                detallesProcesados.add(nuevoDetalle);
            }
        }
        pago.getDetallePagos().addAll(detallesProcesados);
        Pago pagoPersistido = (pago.getId() == null) ? persistAndFlushPago(pago) : mergeAndFlushPago(pago);
        recalcularTotalesNuevo(pagoPersistido);
        return pagoPersistido;
    }

    private Pago persistAndFlushPago(Pago pago) {
        entityManager.persist(pago);
        entityManager.flush();
        return pago;
    }

    private Pago mergeAndFlushPago(Pago pago) {
        Pago pagoMerge = entityManager.merge(pago);
        entityManager.flush();
        return pagoMerge;
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
    }

    private void actualizarDetalleDesdeRequest(DetallePago persistido, DetallePago request) {
        persistido.setDescripcionConcepto(request.getDescripcionConcepto());
        persistido.setACobrar(request.getACobrar());
        persistido.setImporteInicial(request.getImporteInicial());
        if (request.getImportePendiente() != null) {
            persistido.setImportePendiente(request.getImportePendiente() - persistido.getACobrar());
        }
        persistido.setValorBase(request.getValorBase());
        persistido.setTipo(request.getTipo());
        persistido.setTieneRecargo(request.getTieneRecargo());
        persistido.setFechaRegistro(LocalDate.now());
        persistido.setRecargo(request.getRecargo());
    }

    /**
     * Cierra un pago marcándolo como HISTORICO y poniendo el saldo a 0; marca cada detalle como cobrado.
     */
    @Transactional
    protected void cerrarPagoHistorico(Pago pago) {
        pago.setEstadoPago(EstadoPago.HISTORICO);
        pago.setSaldoRestante(0.0);
        for (DetallePago dp : pago.getDetallePagos()) {
            dp.setFechaRegistro(pago.getFecha());
            dp.setCobrado(true);
            dp.setImportePendiente(0.0);
            dp.setUsuario(pago.getUsuario());
            dp.setEstadoPago(EstadoPago.HISTORICO);
            entityManager.merge(dp);
        }
        entityManager.merge(pago);
        entityManager.flush();
        log.info("[cerrarPagoHistorico] Pago id={} marcado como HISTORICO", pago.getId());
    }

    /**
     * Asigna el método de pago al pago, recalcula totales y actualiza el pago.
     */
    @Transactional
    protected void asignarMetodoYPersistir(Pago pago, Long metodoPagoId) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }
        MetodoPago metodoPago = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseGet(() -> metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO"));
        pago.setMetodoPago(metodoPago);
        pagoRepositorio.saveAndFlush(pago);
        if (pago.getDetallePagos().stream().anyMatch(DetallePago::getTieneRecargo)) {
            double recargo = metodoPago.getRecargo() != null ? metodoPago.getRecargo() : 0;
            log.info("[asignarMetodoYPersistir] Se aplicó recargo de {}.", recargo);
        }
        pagoRepositorio.saveAndFlush(pago);
    }

    /**
     * Procesa el abono parcial:
     * 1. Clona los detalles pendientes del pago activo en un nuevo pago.
     * 2. Actualiza (o crea) los detalles del histórico con los abonos.
     * 3. Cierra el pago activo marcándolo como HISTORICO.
     * Retorna el nuevo pago que contiene los detalles pendientes.
     */
    @Transactional
    public Pago procesarAbonoParcial(Pago pagoActivo, PagoRegistroRequest request) {
        log.info("[procesarAbonoParcial] INICIO - Procesando abono parcial para Pago ID: {}", pagoActivo.getId());
        Pago nuevoPago = clonarDetallesConPendiente(pagoActivo, request);
        Pago pagoHistoricoActualizado = actualizarPagoHistoricoConAbonos(nuevoPago, request);
        MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                .orElseGet(() -> metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO"));
        nuevoPago.setMetodoPago(metodoPago);
        recalcularTotalesNuevo(nuevoPago);
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findById(request.usuarioId());
        Usuario cobrador = usuarioOpt.get();
        pagoHistoricoActualizado.setUsuario(cobrador);
        Optional<MetodoPago> metodoPagoOpt = metodoPagoRepositorio.findById(request.metodoPagoId());
        pagoHistoricoActualizado.setMetodoPago(metodoPagoOpt.get());
        cerrarPagoHistorico(pagoActivo);
        pagoHistoricoActualizado = pagoRepositorio.save(pagoHistoricoActualizado);
        nuevoPago.setMetodoPago(metodoPagoOpt.get());
        pagoActivo.setObservaciones(request.observaciones());
        nuevoPago.setUsuario(cobrador);
        log.info("[procesarAbonoParcial] Retornando nuevo pago con detalles pendientes - ID: {}, Estado: {}",
                nuevoPago.getId(), nuevoPago.getEstadoPago());
        return nuevoPago;
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

}
