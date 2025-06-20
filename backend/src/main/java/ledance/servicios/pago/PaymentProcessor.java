package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.dto.pago.DetallePagoResolucion;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.detallepago.DetallePagoResolver;
import ledance.servicios.detallepago.DetallePagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Refactor del servicio PaymentProcessor.
 * Se han centralizado las operaciones clave:
 * - Recalcular totales en nuevo pago.
 * - Procesar abonos parciales, clonacion de detalles pendientes y cierre del pago historico.
 * - Procesar y calcular cada DetallePago de forma unificada.
 */
@Service
@Transactional
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final DetallePagoRepositorio detallePagoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final DetallePagoServicio detallePagoServicio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final RecargoRepositorio recargoRepositorio;
    private final DetallePagoResolver detallePagoResolver;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final MatriculaRepositorio matriculaRepositorio;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final StockRepositorio stockRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio,
                            UsuarioRepositorio usuarioRepositorio, DetallePagoServicio detallePagoServicio, BonificacionRepositorio bonificacionRepositorio, RecargoRepositorio recargoRepositorio, DetallePagoResolver detallePagoResolver, SubConceptoRepositorio subConceptoRepositorio, ConceptoRepositorio conceptoRepositorio, MatriculaRepositorio matriculaRepositorio, MensualidadRepositorio mensualidadRepositorio, StockRepositorio stockRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
        this.detallePagoResolver = detallePagoResolver;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.matriculaRepositorio = matriculaRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.stockRepositorio = stockRepositorio;
    }

    /**
     * Recalcula totales para un pago, ignorando detalles no ACTIVO o removidos.
     */
    public void recalcularTotalesNuevo(Pago pago) {
        log.info("[recalcularTotalesNuevo] Pago id={}", pago.getId());

        if (pago.getMetodoPago() == null) {
            MetodoPago efectivo = metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO");
            pago.setMetodoPago(efectivo);
        }

        double totalDetalles = 0.0;
        double totalCobrado = 0.0;

        for (DetallePago d : pago.getDetallePagos()) {
            if (Boolean.TRUE.equals(d.getRemovido()) || EstadoPago.ANULADO.equals(d.getEstadoPago())) continue;

            d.setFechaRegistro(pago.getFecha());

            totalDetalles += Optional.ofNullable(d.getImporteInicial()).orElse(0.0);
            totalCobrado += Optional.ofNullable(d.getACobrar()).orElse(0.0);

        }

        boolean incluirRecargoMetodo = Boolean.TRUE.equals(pago.getRecargoMetodoPagoAplicado());
        double recargoMetodo = incluirRecargoMetodo
                ? Optional.ofNullable(pago.getMetodoPago().getRecargo()).orElse(0.0)
                : 0.0;
        double montoFinal = totalCobrado + recargoMetodo;
        pago.setMonto(montoFinal);
        pago.setMontoPagado(montoFinal);

        pago.setSaldoRestante(Math.max(0.0, totalDetalles - totalCobrado));
        pago.setEstadoPago(pago.getSaldoRestante() <= 0 ? EstadoPago.HISTORICO : EstadoPago.ACTIVO);

        pagoRepositorio.save(pago);

        log.info("[recalcularTotalesNuevo] totalDetalles={}, totalCobrado={}, recargoMetodo={}, montoFinal={}, estado={}",
                totalDetalles, totalCobrado, recargoMetodo, montoFinal, pago.getEstadoPago());
    }

    /**
     * Crea un DetallePago nuevo a partir de un DetallePagoRegistroRequest.
     */
    /// Refactorizado para evitar duplicar DetallePago con removido = true
    private DetallePago crearNuevoDetalleFromRequest(DetallePago req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] INICIO - Creando detalle desde request. Pago ID: {}", pago.getId());

        DetallePago detalle = new DetallePago();
        // Forzar generación automática si id == 0
        if (req.getId() != null && req.getId() == 0) {
            detalle.setId(null);
        }
        detalle.setAlumno(pago.getAlumno());
        detalle.setPago(pago);

        // Normalizar descripción
        String descripcion = req.getDescripcionConcepto().trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        detalle.setValorBase(req.getValorBase());
        detalle.setCuotaOCantidad(req.getCuotaOCantidad());

        // 1) resolver tipo e IDs
        DetallePagoResolucion res = detallePagoResolver.resolver(descripcion);

        // 2) poblar con el helper
        populate(detalle, res);

        if (detalle.getTipo().equals(TipoDetallePago.STOCK)) {
            paymentCalculationServicio.calcularStock(detalle);
        }

        // Bonificación
        if (req.getBonificacion() != null) {
            Bonificacion bonificacion = obtenerBonificacionPorId(req.getBonificacion().getId());
            detalle.setBonificacion(bonificacion);
        }
        // Recargo
        if (req.getRecargo() != null) {
            Recargo recargo = obtenerRecargoPorId(req.getRecargo().getId());
            detalle.setRecargo(recargo);
        } else {
            detalle.setTieneRecargo(false);
        }

        // Si está marcado como removido, copiar valores del request sin cálculos adicionales
        if (Boolean.TRUE.equals(req.getRemovido())) {
            detalle.setImporteInicial(req.getImporteInicial());
            detalle.setACobrar(Optional.ofNullable(req.getACobrar()).orElse(0.0));
            detalle.setImportePendiente(Optional.ofNullable(req.getImportePendiente()).orElse(0.0));
            detalle.setCobrado(detalle.getImportePendiente() <= 0.0);
            log.info("[crearNuevoDetalleFromRequest] Removido: copia datos sin cálculo. Cobrado: {}", detalle.getCobrado());
            return detalle;
        }

        // Cálculos estándar
        detallePagoServicio.calcularImporte(detalle);
        double aCobrar = Optional.ofNullable(req.getACobrar()).orElse(0.0);
        detalle.setACobrar(aCobrar);
        if (req.getImportePendiente() != null) {
            detalle.setImportePendiente(req.getImportePendiente() - aCobrar);
        }
        detalle.setCobrado(detalle.getImportePendiente() <= 0.0);

        log.info("[crearNuevoDetalleFromRequest] FIN - Detalle creado. Tipo: {}, Cobrado: {}",
                detalle.getTipo(), detalle.getCobrado());
        return detalle;
    }

    private Bonificacion obtenerBonificacionPorId(Long id) {
        return bonificacionRepositorio.findById(id).orElse(null);
    }

    private Recargo obtenerRecargoPorId(Long id) {
        return recargoRepositorio.findById(id).orElse(null);
    }

    private void actualizarDetalleOriginal(DetallePago original) {
        original.setImportePendiente(0.0);
        original.setEstadoPago(EstadoPago.HISTORICO); // Opcional, pero recomendable si querés marcarlo "cerrado"

        String descripcion = original.getDescripcionConcepto().trim().toUpperCase();

        // 1) resolver tipo e IDs
        DetallePagoResolucion res = detallePagoResolver.resolver(descripcion);

        // 2) poblar con el helper
        populate(original, res);
        detallePagoRepositorio.save(original);
    }

    /**
     * Cierra un pago marcandolo como HISTORICO y poniendo el saldo a 0; marca cada detalle como cobrado.
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
     * Asigna el metodo de pago al pago, recalcula totales y actualiza el pago.
     */
    @Transactional
    protected void asignarMetodoYPersistir(Pago pago, Long metodoPagoId, Boolean aplicarRecargo) {
        MetodoPago mp = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseGet(() -> metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO"));

        pago.setMetodoPago(mp);
        pago.setRecargoMetodoPagoAplicado(Boolean.TRUE.equals(aplicarRecargo));

        // (Opcional) Anexa en observaciones por legibilidad
        if (Boolean.TRUE.equals(pago.getRecargoMetodoPagoAplicado())) {
            String obs = Optional.ofNullable(pago.getObservaciones()).orElse("");
            pago.setObservaciones(obs + (obs.isEmpty() ? "" : "\n") + mp.getDescripcion());
        }

        // **Aquí** recalculas y guardas el pago completo con el recargo
        recalcularTotalesNuevo(pago);
    }

    /**
     * Procesa el abono parcial:
     * 1. Clona los detalles pendientes del pago activo en un nuevo pago.
     * 2. Actualiza (o crea) los detalles del historico con los abonos.
     * 3. Cierra el pago activo marcandolo como HISTORICO.
     * Retorna el nuevo pago que contiene los detalles pendientes.
     */
    @Transactional
    public Pago procesarAbonoParcial(Pago pagoActivo, PagoRegistroRequest request) {
        log.info("[procesarAbonoParcial] INICIO - Pago ID: {}", pagoActivo.getId());

        // 1) Cabecera del nuevo pago
        double sumaAbonos = request.detallePagos().stream()
                .mapToDouble(d -> Optional.ofNullable(d.ACobrar()).orElse(0.0))
                .sum();

        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoActivo.getAlumno());
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        Usuario usuario = usuarioRepositorio.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        nuevoPago.setUsuario(usuario);
        nuevoPago.setImporteInicial(sumaAbonos);
        nuevoPago.setMonto(sumaAbonos);
        nuevoPago.setDetallePagos(new ArrayList<>());

        // persisto la cabecera
        nuevoPago = pagoRepositorio.save(nuevoPago);

        // 2) Para cada línea de abono del request
        for (DetallePagoRegistroRequest req : request.detallePagos()) {
            // 2a) Si viene de un detalle existente, archivarlo
            if (req.id() != null && req.id() > 0) {
                DetallePago original = detallePagoRepositorio.findById(req.id())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "DetallePago no encontrado: " + req.id()));

                // Lo dejamos histórico y sin remanente
                original.setImportePendiente(0.0);
                original.setCobrado(true);
                original.setEstadoPago(EstadoPago.HISTORICO);
                String descripcion = original.getDescripcionConcepto();

                DetallePagoResolucion res = detallePagoResolver.resolver(descripcion);
                populate(original, res);

                detallePagoRepositorio.save(original);
            }

            // 2b) Ahora **creo siempre un nuevo DetallePago** para el abono
            DetallePago abono = new DetallePago();
            abono.setPago(nuevoPago);
            abono.setAlumno(nuevoPago.getAlumno());

            // Copio descripción, base y tipo del request (no del original)
            String descripcion = req.descripcionConcepto().trim().toUpperCase();
            abono.setDescripcionConcepto(descripcion);
            abono.setValorBase(req.valorBase());
            abono.setCuotaOCantidad(req.cuotaOCantidad());


            // 1) resolver tipo e IDs
            DetallePagoResolucion res = detallePagoResolver.resolver(descripcion);

            // 2) poblar con el helper
            populate(abono, res);

            if (abono.getTipo().equals(TipoDetallePago.STOCK)) {
                paymentCalculationServicio.calcularStock(abono);
            }

            // importes
            double aCobrar = Optional.ofNullable(req.ACobrar()).orElse(0.0);
            abono.setImporteInicial(aCobrar);
            abono.setACobrar(aCobrar);

            // calculamos pendiente (solo si viene en el request)
            double pendienteReq = Optional.ofNullable(req.importePendiente())
                    .orElse(0.0);
            double nuevoPendiente = pendienteReq - aCobrar;
            abono.setImportePendiente(Math.max(0.0, nuevoPendiente));

            boolean yaCobrado = nuevoPendiente <= 0.0;
            abono.setCobrado(yaCobrado);
            abono.setEstadoPago(yaCobrado
                    ? EstadoPago.HISTORICO
                    : EstadoPago.ACTIVO);

            abono.setUsuario(usuario);

            // persistimos el abono
            detallePagoRepositorio.save(abono);
            nuevoPago.getDetallePagos().add(abono);
        }

        // 3) Si el pagoActivo original quedan todos sus detalles cobrados, ciérralo
        boolean todos = pagoActivo.getDetallePagos().stream()
                .allMatch(DetallePago::getCobrado);
        if (todos) {
            cerrarPagoHistorico(pagoActivo);
        }
        // 4) recalcular totales del nuevo pago
        recalcularTotalesNuevo(nuevoPago);

        log.info("[procesarAbonoParcial] FIN - Nuevo Pago ID: {}", nuevoPago.getId());
        return nuevoPago;
    }

    // 1. Obtener el ultimo pago pendiente (se mantiene similar, verificando saldo > 0)
    @Transactional
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        log.info("[obtenerUltimoPagoPendienteEntidad] Buscando el ultimo pago pendiente para alumnoId={}", alumnoId);
        return pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
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
    public Pago processDetallesPago(Pago pago,
                                    List<DetallePago> detallesFront,
                                    Alumno alumno) {
        // 0) limpiar la colección sin reemplazar la instancia
        if (pago.getDetallePagos() == null) {
            pago.setDetallePagos(new ArrayList<>());
        } else {
            pago.getDetallePagos().clear();
        }

        // 1) asignar el alumno al pago
        pago.setAlumno(alumno);

        // 2) persistir el pago sin hijos
        if (pago.getId() == null) {
            entityManager.persist(pago);
            entityManager.flush();
        }

        // 3) crear y persistir los detalles nuevos
        for (DetallePago req : detallesFront) {
            if (req.getId() != null && req.getId() > 0) {
                DetallePago original = detallePagoRepositorio.findById(req.getId())
                        .orElseThrow(() -> new EntityNotFoundException("DetallePago no encontrado id=" + req.getId()));
                actualizarDetalleOriginal(original);
            }

            DetallePago detalle = crearNuevoDetalleFromRequest(req, pago);

            entityManager.persist(detalle);
            // añadilo a la colección existente
            pago.getDetallePagos().add(detalle);
        }

        // 4) merge + flush para actualizar las referencias
        Pago pagoFinal = entityManager.merge(pago);
        entityManager.flush();

        // 5) recalcular totales
        recalcularTotalesNuevo(pagoFinal);
        return pagoFinal;
    }

    /**
     * Toma un DetallePago ya creado y un DetallePagoResolucion con tipo e IDs resueltos,
     * y le inyecta las entidades correspondientes si el ID no es null.
     */
    public void populate(DetallePago detalle, DetallePagoResolucion res) {
        // 1) establecer el tipo
        detalle.setTipo(res.tipo());

        // 2) SubConcepto
        Optional.ofNullable(res.subConceptoId())
                .ifPresent(id -> detalle.setSubConcepto(
                        subConceptoRepositorio.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("SubConcepto " + id + " no existe"))
                ));

        // 3) Concepto
        Optional.ofNullable(res.conceptoId())
                .ifPresent(id -> detalle.setConcepto(
                        conceptoRepositorio.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Concepto " + id + " no existe"))
                ));

        // 4) Matrícula
        Optional.ofNullable(res.matriculaId())
                .ifPresent(id -> detalle.setMatricula(
                        matriculaRepositorio.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Matrícula " + id + " no existe"))
                ));

        // 5) Mensualidad
        Optional.ofNullable(res.mensualidadId())
                .ifPresent(id -> detalle.setMensualidad(
                        mensualidadRepositorio.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Mensualidad " + id + " no existe"))
                ));

        // 6) Stock
        Optional.ofNullable(res.stockId())
                .ifPresent(id -> detalle.setStock(
                        stockRepositorio.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Stock " + id + " no existe"))
                ));
    }
}
