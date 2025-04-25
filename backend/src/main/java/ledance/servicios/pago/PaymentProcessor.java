package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio,
                            UsuarioRepositorio usuarioRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.usuarioRepositorio = usuarioRepositorio;
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
        }

        double totalCobrar = 0.0;
        double totalBasePend = 0.0;

        // 2. Recorrer detalles vigentes
        for (DetallePago d : pagoNuevo.getDetallePagos()) {
            if (EstadoPago.ANULADO.equals(d.getEstadoPago())) continue;

            d.setFechaRegistro(pagoNuevo.getFecha());
            totalCobrar += Optional.ofNullable(d.getACobrar()).orElse(0.0);
            totalBasePend += Optional.ofNullable(d.getImporteInicial()).orElse(0.0);
        }

        // 3. Marcar histórico si ya quedó pagado
        for (DetallePago d : pagoNuevo.getDetallePagos()) {
            if (EstadoPago.ANULADO.equals(d.getEstadoPago())) continue;
            if (d.getImportePendiente() != null && d.getImportePendiente() <= 0) {
                d.setCobrado(true);
                d.setImportePendiente(0.0);
                d.setEstadoPago(EstadoPago.HISTORICO);
                if (d.getTipo() == TipoDetallePago.MATRICULA && d.getMatricula() != null) {
                    d.getMatricula().setPagada(true);
                }
            }
        }

        // 4. Recargo global
        double recargo = pagoNuevo.getDetallePagos().stream()
                .filter(d -> !EstadoPago.ANULADO.equals(d.getEstadoPago()) && Boolean.TRUE.equals(d.getTieneRecargo()))
                .findAny()
                .map(d -> pagoNuevo.getMetodoPago().getRecargo())
                .orElse(0.0);

        // 5. Montos finales
        pagoNuevo.setMonto(totalCobrar + recargo);
        pagoNuevo.setMontoPagado(totalCobrar + recargo);

        // 6. Saldo restante = suma de BASES – lo cobrado
        double restante = Math.max(0.0, totalBasePend - totalCobrar);
        pagoNuevo.setSaldoRestante(restante);

        // 7. Estado del pago
        pagoNuevo.setEstadoPago(restante <= 0
                ? EstadoPago.HISTORICO
                : EstadoPago.ACTIVO);

        pagoRepositorio.save(pagoNuevo);
        log.info("[PaymentProcessor] Pago id={} → monto={}, saldo={}, estado={}",
                pagoNuevo.getId(),
                pagoNuevo.getMonto(),
                pagoNuevo.getSaldoRestante(),
                pagoNuevo.getEstadoPago());
    }

    /**
     * Procesa los detalles de pago recibidos (del frontend) y los asocia al pago.
     */
    // --------------------------------------------------
    // 5) Procesa todos los detalles de un Pago antes de persistir
    @Transactional
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumno) {
        pago.setAlumno(alumno);
        pago.setDetallePagos(new ArrayList<>());

        List<DetallePago> procesados = new ArrayList<>();
        for (DetallePago req : detallesFront) {
            // 🔥 Crear nuevo detalle
            DetallePago entidad = new DetallePago();
            copiarAtributosDetalle(entidad, req);

            // 🔥 Seteos básicos
            entidad.setAlumno(alumno);
            entidad.setPago(pago);
            entidad.setUsuario(pago.getUsuario());
            entidad.setFechaRegistro(LocalDate.now());
            entidad.setTipo(paymentCalculationServicio.determinarTipoDetalle(entidad.getDescripcionConcepto()));

            // 🔥 Persistir Pago si es nuevo
            if (pago.getId() == null) {
                entityManager.persist(pago);
                entityManager.flush();
            }

            // 🔥 Asociaciones y cálculos
            paymentCalculationServicio.reatacharAsociaciones(entidad, pago);
            Inscripcion insc = obtenerInscripcion(entidad);
            paymentCalculationServicio.procesarYCalcularDetalle(entidad, insc);

            // ✅ 🔥 MARCAR el Detalle original como HISTORICO
            if (req.getId() != null) {
                DetallePago original = detallePagoRepositorio.findById(req.getId())
                        .orElseThrow(() -> new EntityNotFoundException("DetallePago original no encontrado id=" + req.getId()));
                actualizarDetalleOriginal(original);
            }
            procesados.add(entidad);
        }

        pago.getDetallePagos().addAll(procesados);
        Pago finalPago = (pago.getId() == null) ? persistAndFlushPago(pago) : mergeAndFlushPago(pago);

        recalcularTotalesNuevo(finalPago);
        return finalPago;
    }

    private void actualizarDetalleOriginal(DetallePago original) {
        original.setImportePendiente(0.0);
        original.setEstadoPago(EstadoPago.HISTORICO); // Opcional, pero recomendable si querés marcarlo "cerrado"
        detallePagoRepositorio.save(original);
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

    // --------------------------------------------------
    // 1) Copia estricta de todos los campos editables del DTO
    void copiarAtributosDetalle(DetallePago destino, DetallePago origen) {
        // Asociaciones básicas
        destino.setAlumno(origen.getAlumno());
        destino.setPago(origen.getPago());
        destino.setUsuario(origen.getUsuario());
        // Campos que vienen del frontend
        destino.setConcepto(origen.getConcepto());
        destino.setSubConcepto(origen.getSubConcepto());
        destino.setDescripcionConcepto(origen.getDescripcionConcepto());
        destino.setCuotaOCantidad(origen.getCuotaOCantidad());
        destino.setValorBase(origen.getValorBase());
        destino.setImporteInicial(origen.getImporteInicial());
        destino.setImportePendiente(origen.getImportePendiente());
        destino.setACobrar(origen.getACobrar());
        destino.setTieneRecargo(origen.getTieneRecargo());
        destino.setRecargo(origen.getRecargo());
        destino.setBonificacion(origen.getBonificacion());
        destino.setTipo(origen.getTipo());
        destino.setStock(origen.getStock());
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
            log.info("[asignarMetodoYPersistir] Se aplico recargo de {}.", recargo);
        }
        pagoRepositorio.saveAndFlush(pago);
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

        // 1) CABECERA DEL NUEVO PAGO: inicializo con la suma de todos los aCobrar del request
        double sumaAbonos = request.detallePagos().stream()
                .mapToDouble(d -> Optional.ofNullable(d.ACobrar()).orElse(0.0))
                .sum();

        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoActivo.getAlumno());
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setUsuario(usuarioRepositorio.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado")));
        nuevoPago.setMetodoPago(metodoPagoRepositorio.findById(request.metodoPagoId())
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado")));
        nuevoPago.setImporteInicial(sumaAbonos);
        nuevoPago.setMonto(sumaAbonos);
        // arranco la lista de detalles vacía
        nuevoPago.setDetallePagos(new ArrayList<>());

        // persisto la cabecera para obtener ID
        nuevoPago = pagoRepositorio.save(nuevoPago);

        // 2) DETALLES DE ABONO: por cada línea de abono del request...
        for (DetallePagoRegistroRequest req : request.detallePagos()) {
            DetallePago original = detallePagoRepositorio.findById(req.id())
                    .orElseThrow(() -> new EntityNotFoundException("DetallePago no encontrado: " + req.id()));

            double pagoAbono = Optional.ofNullable(req.ACobrar()).orElse(0.0);

            // — creo el detalle de abono
            DetallePago abono = new DetallePago();
            abono.setPago(nuevoPago);
            abono.setAlumno(pagoActivo.getAlumno());
            abono.setDescripcionConcepto(original.getDescripcionConcepto());
            abono.setTipo(original.getTipo());
            abono.setValorBase(original.getValorBase());
            abono.setImporteInicial(pagoAbono);
            abono.setACobrar(pagoAbono);
            abono.setImportePendiente(0.0);
            abono.setCobrado(true);
            abono.setEstadoPago(EstadoPago.HISTORICO);
            // guarda y añade a la lista de nuevos detalles
            detallePagoRepositorio.save(abono);
            nuevoPago.getDetallePagos().add(abono);

            // — ajusto el saldo del original
            double restante = original.getImportePendiente() - pagoAbono;
            original.setImportePendiente(Math.max(0.0, restante));
            if (original.getImportePendiente() <= 0) {
                original.setCobrado(true);
                original.setEstadoPago(EstadoPago.HISTORICO);
            }
            detallePagoRepositorio.save(original);
        }

        // 3) SI todos los detalles del pago original ahora están cobrado, lo cierro
        boolean todos = pagoActivo.getDetallePagos().stream().allMatch(DetallePago::getCobrado);
        if (todos) cerrarPagoHistorico(pagoActivo);

        // 4) RECALCULAR TOTALES DEL NUEVO PAGO — ahora sí encuentra los abonos en detallePagos
        recalcularTotalesNuevo(nuevoPago);

        log.info("[procesarAbonoParcial] FIN - Nuevo Pago ID: {}, monto={}, importeInicial={}",
                nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getImporteInicial());
        return nuevoPago;
    }

    /**
     * Obtiene la inscripcion asociada al detalle, si aplica.
     *
     * @param detalle el objeto DetallePago.
     * @return la Inscripcion asociada o null.
     */
    public Inscripcion obtenerInscripcion(DetallePago detalle) {
        log.info("[obtenerInscripcion] Buscando inscripcion para DetallePago id={}", detalle.getId());
        if (detalle.getDescripcionConcepto().contains("CUOTA") && detalle.getMensualidad() != null &&
                detalle.getMensualidad().getInscripcion() != null) {
            return detalle.getMensualidad().getInscripcion();
        }
        return null;
    }

    // 1. Obtener el ultimo pago pendiente (se mantiene similar, verificando saldo > 0)
    @Transactional
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        log.info("[obtenerUltimoPagoPendienteEntidad] Buscando el ultimo pago pendiente para alumnoId={}", alumnoId);
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
