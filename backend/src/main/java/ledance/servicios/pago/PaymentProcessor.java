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

import java.time.LocalDate;
import java.util.*;

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
    private final AlumnoRepositorio alumnoRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            DetallePagoServicio detallePagoServicio,
                            BonificacionRepositorio bonificacionRepositorio,
                            RecargoRepositorio recargoRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio,
                            UsuarioRepositorio usuarioRepositorio, AlumnoRepositorio alumnoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
    }

    /**
     * Recalcula totales para un pago, ignorando detalles no ACTIVO o removidos.
     */
    @Transactional
    public void recalcularTotalesNuevo(Pago pagoNuevo) {
        log.info("[PaymentProcessor] recalcularTotalesNuevo pago id={}", pagoNuevo.getId());

        // 1. Asegurar método de pago
        if (pagoNuevo.getMetodoPago() == null) {
            MetodoPago efectivo = metodoPagoRepositorio
                    .findByDescripcionContainingIgnoreCase("EFECTIVO");
            pagoNuevo.setMetodoPago(efectivo);
            log.info("  ↳ Se asigna metodo EFECTIVO id={}", efectivo.getId());
        }

        double totalCobrar = 0.0;
        double totalPendiente = 0.0;

        // 2. Recorrer TODOS los detalles (solo ignorar los removidos)
        for (DetallePago d : pagoNuevo.getDetallePagos()) {
            if (Boolean.TRUE.equals(d.getRemovido())) {
                continue;
            }

            // Actualizar fecha de registro al del pago
            d.setFechaRegistro(pagoNuevo.getFecha());

            // Sumar lo que realmente se cobró
            double cobrado = Optional.ofNullable(d.getACobrar())
                    .filter(v -> v > 0)
                    .orElse(0.0);
            totalCobrar += cobrado;

            // Acumular el pendiente actual
            totalPendiente += Optional.ofNullable(d.getImportePendiente())
                    .orElse(0.0);
        }

        // 3. Marcar como histórico y liquidar importes solo después de sumar
        for (DetallePago d : pagoNuevo.getDetallePagos()) {
            if (Boolean.TRUE.equals(d.getRemovido())) continue;

            if (d.getImportePendiente() <= 0) {
                d.setCobrado(true);
                d.setImportePendiente(0.0);
                d.setEstadoPago(EstadoPago.HISTORICO);

                if (d.getTipo() == TipoDetallePago.MATRICULA && d.getMatricula() != null) {
                    d.getMatricula().setPagada(true);
                }
            }
        }

        // 4. Calcular recargo global (si algún detalle lo tuvo)
        double recargo = pagoNuevo.getDetallePagos().stream()
                .filter(DetallePago::getTieneRecargo)
                .findFirst()
                .map(_d -> pagoNuevo.getMetodoPago().getRecargo())
                .orElse(0.0);

        // 5. Asignar montos finales
        double montoFinal = totalCobrar + recargo;
        pagoNuevo.setMonto(montoFinal);
        pagoNuevo.setMontoPagado(montoFinal);

        // El saldo restante es el acumulado de pendientes
        pagoNuevo.setSaldoRestante(totalPendiente <= 0 ? 0.0 : totalPendiente);

        // Estado del pago según pendientes
        pagoNuevo.setEstadoPago(totalPendiente <= 0
                ? EstadoPago.HISTORICO
                : EstadoPago.ACTIVO);

        // 6. Persistir cambios
        pagoRepositorio.save(pagoNuevo);
        log.info("[PaymentProcessor] Pago id={} actualizado: monto={}, saldo={}, estado={}",
                pagoNuevo.getId(),
                pagoNuevo.getMonto(),
                pagoNuevo.getSaldoRestante(),
                pagoNuevo.getEstadoPago());
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
            nuevoDetalle.setACobrar(detalleReqOpt.map(DetallePagoRegistroRequest::ACobrar).orElse(detalle.getACobrar()));
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

        // 1) Limpiar o inicializar la lista de detalles
        if (pago.getDetallePagos() == null) {
            pago.setDetallePagos(new ArrayList<>());
        } else {
            pago.getDetallePagos().clear();
        }

        List<DetallePago> detallesProcesados = new ArrayList<>();

        // 2) Procesar cada request de detalle
        for (DetallePago detalleReq : detallesFront) {
            // reatachar alumno/pago/usuario
            detalleReq.setAlumno(alumnoPersistido);
            detalleReq.setPago(pago);
            detalleReq.setUsuario(pago.getUsuario());

            DetallePago detalle;
            if (detalleReq.getId() != null && detalleReq.getId() > 0) {
                // rama EXISTENTE
                detalle = detallePagoRepositorio.findById(detalleReq.getId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "DetallePago no encontrado para ID " + detalleReq.getId()));
                actualizarDetalleDesdeRequest(detalle, detalleReq);
            } else {
                // rama NUEVO
                detalle = new DetallePago();
                copiarAtributosDetalle(detalle, detalleReq);
                detalle.setFechaRegistro(LocalDate.now());
                TipoDetallePago tipo = paymentCalculationServicio
                        .determinarTipoDetalle(detalle.getDescripcionConcepto());
                detalle.setTipo(tipo);
            }

            // 3) Persistir pago si es la primera vez
            if (pago.getId() == null) {
                entityManager.persist(pago);
                entityManager.flush();
            }

            // 4) Reatachar asociaciones JPA y calcular importe
            paymentCalculationServicio.reatacharAsociaciones(detalle, pago);
            Inscripcion insc = obtenerInscripcion(detalle);
            paymentCalculationServicio.procesarYCalcularDetalle(detalle, insc);

            // 5) AHORA: si es matrícula, consumir crédito del alumno
            if (detalle.getTipo() == TipoDetallePago.MATRICULA) {
                paymentCalculationServicio.aplicarDescuentoCreditoEnMatricula(pago, detalle);
                // y guardar el alumno con crédito ya descontado
                alumnoRepositorio.save(alumnoPersistido);
            }

            detallesProcesados.add(detalle);
        }

        // 6) Asociar todos los detalles al pago y persistirlo
        pago.getDetallePagos().addAll(detallesProcesados);
        Pago pagoPersistido = (pago.getId() == null)
                ? persistAndFlushPago(pago)
                : mergeAndFlushPago(pago);

        // 7) Recalcular totales teniendo en cuenta el crédito ya aplicado
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

        // 1) Clonar los detalles pendientes al nuevo pago
        Pago nuevoPago = clonarDetallesConPendiente(pagoActivo, request);

        // 2) Actualizar el pago histórico con los abonos
        Pago pagoHistorico = actualizarPagoHistoricoConAbonos(nuevoPago, request);

        // 3) Marcar el pago activo como histórico
        cerrarPagoHistorico(pagoActivo);

        // 4) Cargar usuario cobrador y método de pago
        Usuario cobrador = usuarioRepositorio.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                .orElseGet(() -> metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO"));

        // 5) Asignar usuario y método al pago histórico y persistirlo
        pagoHistorico.setUsuario(cobrador);
        pagoHistorico.setMetodoPago(metodo);
        pagoRepositorio.save(pagoHistorico);

        // 6) Asignar usuario y método al nuevo pago
        nuevoPago.setUsuario(cobrador);
        nuevoPago.setMetodoPago(metodo);

        // 7) Recalcular totales del nuevo pago
        recalcularTotalesNuevo(nuevoPago);

        // 8) Persistir y devolver el nuevo pago
        Pago pagoProcesado = pagoRepositorio.save(nuevoPago);
        log.info("[procesarAbonoParcial] FIN - Retornando nuevo pago con detalles pendientes - ID: {}, Estado: {}",
                pagoProcesado.getId(), pagoProcesado.getEstadoPago());

        return pagoProcesado;
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
