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
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final DetallePagoServicio detallePagoServicio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final RecargoRepositorio recargoRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            DetallePagoRepositorio detallePagoRepositorio,
                            PaymentCalculationServicio paymentCalculationServicio, InscripcionRepositorio inscripcionRepositorio, DisciplinaRepositorio disciplinaRepositorio, DetallePagoServicio detallePagoServicio, BonificacionRepositorio bonificacionRepositorio, RecargoRepositorio recargoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.recargoRepositorio = recargoRepositorio;
    }

    /**
     * Reatacha las asociaciones de un DetallePago (alumno, mensualidad, matricula, stock)
     * para garantizar que las entidades esten en estado managed y evitar errores de detached.
     *
     * @param detalle el objeto DetallePago a reatachar.
     * @param pago    el objeto Pago asociado.
     */
    public void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        log.info("[reatacharAsociaciones] Iniciando reatachamiento de asociaciones para DetallePago id={}", detalle.getId());
        if (detalle.getaCobrar() == null) {
            detalle.setaCobrar(0.0);
            log.info("[reatacharAsociaciones] Se asigna aCobrar=0.0 para DetallePago id={}", detalle.getId());
        }
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
            log.info("[reatacharAsociaciones] Alumno asignado: ID {} al DetallePago ID {}", pago.getAlumno().getId(), detalle.getId());
        }
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getId() != null) {
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
                log.info("[reatacharAsociaciones] Mensualidad reatachada para DetallePago id={}", detalle.getId());
            }
        }
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
                log.info("[reatacharAsociaciones] Matrícula reatachada para DetallePago id={}", detalle.getId());
            }
        }
        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
                log.info("[reatacharAsociaciones] Stock reatachado para DetallePago id={}", detalle.getId());
            }
        }
        log.info("[reatacharAsociaciones] Reatachamiento finalizado para DetallePago id={}", detalle.getId());
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

        double montoFinal = montoTotalAbonado.doubleValue() + saldoTotalPendiente.doubleValue();
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
    public boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        log.info("[esPagoHistoricoAplicable] Iniciando verificación basada únicamente en el importe pendiente.");
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] No se puede aplicar pago histórico: pago o detallePagos es nulo.");
            return false;
        }
        double totalPendienteHistorico = ultimoPendiente.getDetallePagos().stream()
                .filter(detalle -> detalle.getImportePendiente() != null
                        && detalle.getImportePendiente() > 0.0
                        && !detalle.getCobrado())
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        log.info("[esPagoHistoricoAplicable] Total pendiente en pago histórico: {}", totalPendienteHistorico);

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
     * Procesa un único detalle de pago: asigna el alumno, reatacha asociaciones y llama al método
     * que recalcula y procesa el detalle (internamente se invoca a procesarYCalcularDetalle).
     */
    private void procesarDetalle(Pago pago, DetallePago detalle, Alumno alumnoPersistido) {
        log.info("[procesarDetalle] Iniciando procesamiento para DetallePago id={} en el Pago id={}", detalle.getId(), pago.getId());
        detalle.setAlumno(alumnoPersistido);
        log.info("[procesarDetalle] Alumno id={} asignado al detalle id={}", alumnoPersistido.getId(), detalle.getId());
        if (!detalle.getTieneRecargo()) {
            detalle.setRecargo(null);
            log.info("[procesarDetalle] Se asigna recargo null al DetallePago id={} porque tieneRecargo es false", detalle.getId());
        }
        if (detalle.getPago() == null || detalle.getPago().getId() == null || detalle.getPago().getId() == 0) {
            detalle.setPago(pago);
            log.info("[procesarDetalle] Pago id={} asignado al detalle id={}", pago.getId(), detalle.getId());
        }
        reatacharAsociaciones(detalle, pago);
        log.info("[procesarDetalle] Asociaciones reatachadas para el detalle id={}", detalle.getId());
        Inscripcion inscripcion = obtenerInscripcion(detalle);
        if (inscripcion != null) {
            log.info("[procesarDetalle] Inscripción encontrada para detalle id={}", detalle.getId());
        } else {
            log.info("[procesarDetalle] No se encontró inscripción para detalle id={}", detalle.getId());
        }
        paymentCalculationServicio.procesarYCalcularDetalle(pago, detalle, inscripcion);
        log.info("[procesarDetalle] Procesamiento y cálculo finalizado para el detalle id={}", detalle.getId());
    }

    /**
     * Crea un nuevo DetallePago a partir del DTO y del Pago (ya existente).
     * Reutiliza la lógica ya existente para asignar los valores base y calcular importes.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        log.info("[crearNuevoDetalleFromRequest] Iniciando creación de nuevo DetallePago a partir del request: {}", req);
        DetallePago detalle = new DetallePago();
        if (req.id() == 0) {
            detalle.setId(null);
            log.info("[crearNuevoDetalleFromRequest] ID del request era 0, se asigna null al nuevo DetallePago.");
        }
        detalle.setAlumno(pago.getAlumno());
        log.info("[crearNuevoDetalleFromRequest] Alumno asignado al DetallePago: {}", pago.getAlumno());
        detalle.setDescripcionConcepto(req.descripcionConcepto().trim());
        log.info("[crearNuevoDetalleFromRequest] Descripción asignada: '{}'", req.descripcionConcepto().trim());
        detalle.setValorBase(req.valorBase());
        log.info("[crearNuevoDetalleFromRequest] Valor base asignado: {}", req.valorBase());
        detalle.setPago(pago);
        detalle.setCuotaOCantidad(req.cuotaOCantidad());
        String descripcion = detalle.getDescripcionConcepto();
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[crearNuevoDetalleFromRequest] Tipo determinado para DetallePago: {}", tipo);
        if (req.bonificacionId() != null) {
            detalle.setBonificacion(obtenerBonificacionPorId(req.bonificacionId()));
            log.info("[crearNuevoDetalleFromRequest] Bonificación asignada con ID: {}", req.bonificacionId());
        }
        if(req.tieneRecargo()){
            if (req.recargoId() != null) {
                detalle.setRecargo(obtenerRecargoPorId(req.recargoId()));
                log.info("[crearNuevoDetalleFromRequest] Recargo asignado con ID: {}", req.recargoId());
            }
        }
        // Calcular importes
        detallePagoServicio.calcularImporte(detalle);
        log.info("[crearNuevoDetalleFromRequest] Se ha calculado el importe para DetallePago id={}: importeInicial={}, importePendiente={}",
                detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente());
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
        log.info("[crearNuevoDetalleFromRequest] DetallePago marcado como cobrado: {}", detalle.getCobrado());
        return detalle;
    }

    /**
     * Obtiene la inscripción aplicable para el alumno, a partir de la disciplina extraída de la descripción del detalle.
     */
    private Inscripcion obtenerInscripcionSiAplica(Alumno alumno, DetallePago detalle) {
        Disciplina disciplina = extraerDisciplinaDesdeDescripcion(detalle.getDescripcionConcepto());
        if (disciplina != null) {
            Optional<Inscripcion> inscripcionOpt = inscripcionRepositorio
                    .findByAlumnoIdAndDisciplinaIdAndEstado(alumno.getId(), disciplina.getId(), EstadoInscripcion.ACTIVA);
            return inscripcionOpt.orElse(null);
        } else {
            log.warn("No se pudo determinar la disciplina para '{}'", detalle.getDescripcionConcepto());
            return null;
        }
    }

    /**
     * Extrae la disciplina a partir de la descripción (puedes ajustar la lógica según tu necesidad).
     */
    private Disciplina extraerDisciplinaDesdeDescripcion(String descripcion) {
        String nombreDisciplina = paymentCalculationServicio.extraerNombreDisciplina(descripcion);
        return disciplinaRepositorio.findByNombreContainingIgnoreCase(nombreDisciplina);
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
        log.info("[clonarDetallePago] Asignando fechaRegistro.");
        clone.setFechaRegistro(LocalDate.now());

        // IMPORTANTE: Usar el importePendiente restante, NO importeInicial original
        log.info("[clonarDetallePago] Calculando importePendienteRestante.");
        double importePendienteRestante = original.getImportePendiente() != null
                ? original.getImportePendiente()
                : original.getImporteInicial();
        log.info("[clonarDetallePago] Importe pendiente restante calculado: {}", importePendienteRestante);

        log.info("[clonarDetallePago] Asignando importeInicial.");
        clone.setImporteInicial(importePendienteRestante);
        log.info("[clonarDetallePago] Asignando importePendiente.");
        clone.setImportePendiente(importePendienteRestante);

        log.info("[clonarDetallePago] Asignando alumno desde nuevoPago.");
        clone.setAlumno(nuevoPago.getAlumno());

        // Asociaciones (mensualidad solo si aún tiene importe pendiente)
        if (original.getTipo() == TipoDetallePago.MENSUALIDAD && original.getMensualidad() != null
                && original.getMensualidad().getEstado() != EstadoMensualidad.PAGADO) {
            log.info("[clonarDetallePago] Asignando mensualidad (condiciones cumplidas).");
            clone.setMensualidad(original.getMensualidad());
        }
        if (original.getTipo() == TipoDetallePago.MATRICULA && original.getMatricula() != null
                && !original.getMatricula().getPagada()) {
            log.info("[clonarDetallePago] Asignando matricula (condiciones cumplidas).");
            clone.setMatricula(original.getMatricula());
        }

        log.info("[clonarDetallePago] Asignando stock.");
        clone.setStock(original.getStock());

        log.info("[clonarDetallePago] Asignando cobrado. Valor cobrado: {}", (importePendienteRestante == 0));
        clone.setCobrado(importePendienteRestante == 0);

        log.info("[clonarDetallePago] Asignando nuevo pago.");
        clone.setPago(nuevoPago);

        log.info("[clonarDetallePago] Clonación completada, retornando clone.");
        return clone;
    }

    /**
     * Busca un DetallePago existente en base a criterios unicos:
     * - Alumno (a traves del pago)
     * - Descripcion normalizada
     * - Tipo (MENSUALIDAD o MATRICULA)
     * - Asociacion con matricula o mensualidad (segun corresponda)
     * - Estado: cobrado = false
     *
     * @param detalle  el detalle de pago a evaluar.
     * @param alumnoId el ID del alumno asociado.
     * @return el DetallePago encontrado o null si no existe.
     */
    public DetallePago findDetallePagoByCriteria(DetallePago detalle, Long alumnoId) {
        // Se utiliza la descripcion tal como se guarda en la BD (normalizada en la entidad)
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
