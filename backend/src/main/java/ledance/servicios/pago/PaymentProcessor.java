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
        if (detalle.getaCobrar() == null) {
            detalle.setaCobrar(0.0);
        }
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
     * @param pago el objeto Pago al cual se recalcularan sus totales.
     */
    public void recalcularTotales(Pago pago) {
        log.info("[recalcularTotales] Recalculando totales para Pago ID: {}", pago.getId());
        BigDecimal montoTotalAbonado = BigDecimal.ZERO;
        BigDecimal saldoTotal = BigDecimal.ZERO;

        for (DetallePago detalle : pago.getDetallePagos()) {
            detalle.setAlumno(pago.getAlumno());
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

        // Asignar estado según el saldo restante
        if (saldoTotal.compareTo(BigDecimal.ZERO) == 0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
        }
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
    boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        log.info("[esPagoHistoricoAplicable] Iniciando verificacion basada unicamente en el importe pendiente.");

        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] No se puede aplicar pago historico: pago o detallePagos es nulo.");
            return false;
        }

        // Sumar el importe pendiente de todos los detalles del pago historico que no esten marcados como cobrados
        double totalPendienteHistorico = ultimoPendiente.getDetallePagos().stream()
                .filter(detalle -> detalle.getImportePendiente() != null
                        && detalle.getImportePendiente() > 0.0
                        && !detalle.getCobrado())
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        log.info("[esPagoHistoricoAplicable] Total pendiente en pago historico: {}", totalPendienteHistorico);

        // Sumar el total a abonar que se especifica en el request
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

        for (DetallePagoRegistroRequest detalleReq : request.detallePagos()) {
            // Buscamos un detalle existente que cumpla las condiciones:
            // descripción igual (ignorando espacios y mayúsculas), importePendiente > 0 y no cobrado.
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

                // Actualizamos el valor de aCobrar con el monto recibido en la request
                double nuevoACobrar = detalleReq.aCobrar();
                log.info("[actualizarPagoHistoricoConAbonos] Actualizando aCobrar del detalle id={} de {} a {}",
                        detalleExistente.getId(), detalleExistente.getaCobrar(), nuevoACobrar);
                detalleExistente.setaCobrar(nuevoACobrar);

                // Procesamos el detalle (se recalculan descuentos, abonos, etc.)
                procesarDetalle(pagoHistorico, detalleExistente, pagoHistorico.getAlumno());
            } else {
                log.info("[actualizarPagoHistoricoConAbonos] No se encontró detalle existente para '{}'. Creando nuevo detalle.",
                        detalleReq.descripcionConcepto());
                DetallePago nuevoDetalle = crearNuevoDetalleFromRequest(detalleReq, pagoHistorico);
                // Solo agregamos si el nuevo detalle tiene importe pendiente > 0
                if (nuevoDetalle.getImportePendiente() > 0) {
                    log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle '{}' con importePendiente {}. Se agrega al pago.",
                            nuevoDetalle.getDescripcionConcepto(), nuevoDetalle.getImportePendiente());
                    pagoHistorico.getDetallePagos().add(nuevoDetalle);
                    procesarDetalle(pagoHistorico, nuevoDetalle, pagoHistorico.getAlumno());
                } else {
                    log.info("[actualizarPagoHistoricoConAbonos] Nuevo detalle '{}' sin importe pendiente, se omite.",
                            nuevoDetalle.getDescripcionConcepto());
                }
            }
        }

        // Recalcular totales del pago (monto total abonado, saldo restante, etc.)
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

        // Asignar siempre el alumno persistido
        detalle.setAlumno(alumnoPersistido);
        log.info("[procesarDetalle] Alumno id={} asignado al detalle id={}", alumnoPersistido.getId(), detalle.getId());

        // Si el detalle no tiene pago asignado, se asigna el pago actual
        if (detalle.getPago() == null || detalle.getPago().getId() == null || detalle.getPago().getId() == 0) {
            detalle.setPago(pago);
            log.info("[procesarDetalle] Pago id={} asignado al detalle id={}", pago.getId(), detalle.getId());
        }

        // Reatachar asociaciones (por ejemplo, bonificaciones, recargos, etc.)
        reatacharAsociaciones(detalle, pago);
        log.info("[procesarDetalle] Asociaciones reatachadas para el detalle id={}", detalle.getId());

        // Obtener la inscripción aplicable (si corresponde)
        Inscripcion inscripcion = obtenerInscripcion(detalle);
        if (inscripcion != null) {
            log.info("[procesarDetalle] Inscripción encontrada para detalle id={}", detalle.getId());
        } else {
            log.info("[procesarDetalle] No se encontró inscripción para detalle id={}", detalle.getId());
        }

        // Procesar y recalcular el detalle (este método centraliza la lógica de cálculo, descuentos, abonos, etc.)
        paymentCalculationServicio.procesarYCalcularDetalle(pago, detalle, inscripcion);
        log.info("[procesarDetalle] Procesamiento y cálculo finalizado para el detalle id={}", detalle.getId());
    }

    /**
     * Crea un nuevo DetallePago a partir del DTO y del Pago (ya existente).
     * Reutiliza la lógica ya existente para asignar los valores base y calcular importes.
     */
    private DetallePago crearNuevoDetalleFromRequest(DetallePagoRegistroRequest req, Pago pago) {
        DetallePago detalle = new DetallePago();
        // Si el id del request es 0, se fuerza a null para crear un nuevo registro.
        if (req.id() == 0) {
            detalle.setId(null);
        }
        detalle.setAlumno(pago.getAlumno());
        detalle.setDescripcionConcepto(req.descripcionConcepto().trim());
        detalle.setValorBase(req.valorBase()); // valor base necesario para los cálculos
        detalle.setPago(pago);
        detalle.setCuotaOCantidad(req.cuotaOCantidad());
        // Determinar tipo en base a la descripción (puedes encapsular esta lógica en el servicio de cálculo)
        detalle.setTipo(paymentCalculationServicio.determinarTipoDetalle(detalle.getDescripcionConcepto()));
        if (req.bonificacionId() != null) {
            detalle.setBonificacion(obtenerBonificacionPorId(req.bonificacionId()));
        }
        if (req.recargoId() != null) {
            detalle.setRecargo(obtenerRecargoPorId(req.recargoId()));
        }
        // Llamar a calcularImporte para asignar importeInicial e importePendiente
        detallePagoServicio.calcularImporte(detalle);
        // Marca como cobrado si el importe pendiente es 0
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
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
        log.info("[clonarDetallesConPendiente] Iniciando clonación de detalles pendientes del pago histórico, id={}", pagoHistorico.getId());

        // Crear nuevo pago con datos básicos
        Pago nuevoPago = new Pago();
        nuevoPago.setAlumno(pagoHistorico.getAlumno());
        nuevoPago.setFecha(LocalDate.now());
        // Se conserva o ajusta la fecha de vencimiento según lógica de negocio
        nuevoPago.setFechaVencimiento(pagoHistorico.getFechaVencimiento());
        nuevoPago.setDetallePagos(new ArrayList<>());

        // Iterar sobre cada detalle del pago histórico
        for (DetallePago detalle : pagoHistorico.getDetallePagos()) {
            log.info("[clonarDetallesConPendiente] Procesando detalle: id={}, descripción='{}', importePendiente={}",
                    detalle.getId(), detalle.getDescripcionConcepto(), detalle.getImportePendiente());

            // Sólo clonar si tiene saldo pendiente (importePendiente > 0)
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0.0) {
                DetallePago nuevoDetalle = clonarDetallePago(detalle, nuevoPago);
                nuevoPago.getDetallePagos().add(nuevoDetalle);
                log.info("[clonarDetallesConPendiente] Detalle clonado: id antiguo={}, importePendiente={}",
                        detalle.getId(), detalle.getImportePendiente());
            } else {
                log.info("[clonarDetallesConPendiente] Detalle no clonado (saldado): id={}, importePendiente={}",
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
        double importeInicialCalculado = calcularImporteInicialDesdeDetalles(nuevoPago.getDetallePagos());
        nuevoPago.setImporteInicial(importeInicialCalculado);
        log.info("[clonarDetallesConPendiente] Importe inicial calculado para nuevo pago: {}", importeInicialCalculado);

        // Persistir el nuevo pago
        nuevoPago = pagoRepositorio.save(nuevoPago);
        log.info("[clonarDetallesConPendiente] Nuevo pago creado con id={} y {} detalles pendientes",
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
    private DetallePago clonarDetallePago(DetallePago original, Pago nuevoPago) {
        DetallePago clone = new DetallePago();
        // Copiamos las propiedades basicas y de asociacion
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
