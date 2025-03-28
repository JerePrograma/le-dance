package ledance.servicios.pago;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.detallepago.DetallePagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);
    @PersistenceContext
    private EntityManager entityManager;

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    // Servicios para delegar la logica de calculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        DetallePagoMapper detallePagoMapper,
                        RecargoRepositorio recargoRepositorio,
                        BonificacionRepositorio bonificacionRepositorio,
                        PaymentCalculationServicio paymentCalculationServicio,
                        DetallePagoServicio detallePagoServicio,
                        PaymentProcessor paymentProcessor,
                        SubConceptoRepositorio subConceptoRepositorio,
                        ConceptoRepositorio conceptoRepositorio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
    }

    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Iniciando registro de pago. Payload: {}", request);

        Alumno alumnoPersistido = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalStateException("Alumno no encontrado para ID: " + request.alumno().id()));

        Pago pagoFinal;
        Pago ultimoPagoActivo = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoPersistido.getId());
        asignarMetodoYPersistir(ultimoPagoActivo, request.metodoPagoId());

        if (esAbonoParcial(ultimoPagoActivo, request)) {
            pagoFinal = procesarAbonoParcial(ultimoPagoActivo, request);
        } else {
            log.info("[registrarPago] Registro inicial. Creando un nuevo pago para alumno id={}", alumnoPersistido.getId());
            pagoFinal = crearNuevoPago(alumnoPersistido, request);
        }

        asignarMetodoYPersistir(pagoFinal, request.metodoPagoId());
        limpiarAsociacionesParaRespuesta(pagoFinal);
        PagoResponse response = pagoMapper.toDTO(pagoFinal);
        log.info("[registrarPago] Pago registrado con exito. Respuesta final: {}", response);
        return response;
    }

    /**
     * Limpia asociaciones innecesarias para la respuesta (por ejemplo, las inscripciones del alumno).
     */
    private void limpiarAsociacionesParaRespuesta(Pago pago) {
        if (pago.getAlumno() != null && pago.getAlumno().getInscripciones() != null) {
            pago.getAlumno().getInscripciones().clear();
        }
    }

    /**
     * Asigna el metodo de pago al pago, recalcula totales y retorna el pago actualizado.
     */
    private void asignarMetodoYPersistir(Pago pago, Long metodoPagoId) {
        Optional<MetodoPago> metodoPagoOpt = metodoPagoRepositorio.findById(metodoPagoId);
        if (metodoPagoOpt.isPresent()) {
            pago.setMetodoPago(metodoPagoOpt.get());
        } else {
            MetodoPago metodoPago = metodoPagoRepositorio.findByDescripcionContainingIgnoreCase("EFECTIVO");
            pago.setMetodoPago(metodoPago);
        }
    }

    /**
     * Procesa el abono parcial:
     * 1. Actualiza el pago activo con los abonos.
     * 2. Clona los detalles pendientes en un nuevo pago.
     * 3. Marca el pago activo como HISTORICO.
     * Retorna el nuevo pago (con los detalles pendientes) que representa el abono en curso.
     */
    private Pago procesarAbonoParcial(Pago pagoActivo, PagoRegistroRequest request) {
        // Actualizar el pago activo con los abonos del request (sin marcarlo como HISTORICO aun)
        Pago pagoActualizado = paymentProcessor.actualizarPagoHistoricoConAbonos(pagoActivo, request);

        // Intentar clonar los detalles pendientes
        Pago nuevoPago = paymentProcessor.clonarDetallesConPendiente(pagoActualizado);
        marcarPagoComoHistorico(pagoActualizado);
        if (nuevoPago == null) {
            // No hay detalles pendientes: se marca el pago activo como HISTORICO y se retorna este
            log.info("[procesarAbonoParcial] No hay detalles pendientes. Se retorna el pago historico actualizado: {}", pagoActualizado);
            return pagoActualizado;
        } else {
            // Se han clonado detalles pendientes: se marca el pago activo como HISTORICO y se retorna el nuevo pago
            log.info("[procesarAbonoParcial] Nuevo pago generado (con detalles pendientes): {}", nuevoPago);
            return nuevoPago;
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
    private void marcarPagoComoHistorico(Pago pago) {
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
     * Valida que exista un pago activo con saldo pendiente y que se cumpla la logica de migracion.
     */
    private boolean esAbonoParcial(Pago ultimoPagoActivo, PagoRegistroRequest request) {
        return (ultimoPagoActivo != null
                && ultimoPagoActivo.getSaldoRestante() > 0
                && paymentProcessor.esPagoHistoricoAplicable(ultimoPagoActivo, request));
    }

    /**
     * Crea un nuevo pago y procesa los detalles enviados en el request.
     * Se asume que en el registro inicial no se debe verificar duplicados.
     */
    private Pago crearNuevoPago(Alumno alumno, PagoRegistroRequest request) {
        Pago nuevoPago = pagoMapper.toEntity(request);
        if (nuevoPago.getImporteInicial() == null) {
            nuevoPago.setImporteInicial(request.importeInicial());
        }
        nuevoPago.setAlumno(alumno);
        log.info("[crearNuevoPago] Nuevo pago mapeado: fecha={}, fechaVencimiento={}, importeInicial={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getImporteInicial());

        List<DetallePago> detallesFront = detallePagoMapper.toEntity(request.detallePagos());
        // Se procesa el pago con todos los detalles sin filtrado (ya que es registro inicial)
        return processDetallesPago(nuevoPago, detallesFront, alumno);
    }

    /**
     * Metodo unificado para procesar los detalles de pago.
     * Se actualizan los detalles existentes o se crean nuevos segun corresponda.
     *
     * @param pago          el objeto Pago a actualizar o persistir.
     * @param detallesFront lista de DetallePago provenientes del request.
     * @return el objeto Pago actualizado y persistido.
     */
    public Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront, Alumno alumnoPersistido) {
        log.info("[processDetallesPago] Iniciando proceso de DetallesPago");
        log.info("[processDetallesPago] Pago inicial: ID={}, alumno.id={}",
                pago.getId(), (pago.getAlumno() != null ? pago.getAlumno().getId() : "null"));
        log.info("[processDetallesPago] Alumno persistido recibido: ID={}",
                (alumnoPersistido != null ? alumnoPersistido.getId() : "null"));

        // Si el pago ya existe (id distinto de 0) se carga; de lo contrario, se usa el objeto recibido.
        Pago pagoManaged = (pago.getId() != null && pago.getId() != 0)
                ? paymentProcessor.loadAndUpdatePago(pago)
                : pago;
        log.info("[processDetallesPago] Pago gestionado tras carga/verificacion: ID={}", pagoManaged.getId());

        // Corregir id del pago si es 0.
        if (pagoManaged.getId() != null && pagoManaged.getId() == 0) {
            log.info("[processDetallesPago] Pago ID es 0, se setea a null");
            pagoManaged.setId(null);
        }

        // Forzar que el pago tenga asignado el alumno persistido.
        assert alumnoPersistido != null;
        log.info("[processDetallesPago] Asignando alumno persistido al pago: ID={}", alumnoPersistido.getId());
        pagoManaged.setAlumno(alumnoPersistido);

        // NOTA: En lugar de persistir el pago ahora, se pospone la persistencia hasta que se procesen los detalles.

        List<DetallePago> detallesProcesados = new ArrayList<>();

        // Procesamos cada detalle recibido.
        for (DetallePago detalle : detallesFront) {
            log.info("[processDetallesPago] Procesando DetallePago: descripcionConcepto='{}', id={}",
                    detalle.getDescripcionConcepto(), detalle.getId());
            if (detalle.getId() != null && detalle.getId() == 0) {
                log.info("[processDetallesPago] DetallePago con id 0, reinicializando id y version a null");
                detalle.setId(null);
                detalle.setVersion(null);
            }
            // Usar siempre el alumnoPersistido en lugar del que venga del DTO:
            detalle.setAlumno(alumnoPersistido);
            log.info("[processDetallesPago] Alumno asignado al detalle: ID={}",
                    (detalle.getAlumno() != null ? detalle.getAlumno().getId() : "null"));
            // Asignar el pago (aunque aun no tenga ID, se mantiene la asociacion)
            if (detalle.getPago() == null || detalle.getPago().getId() == null || detalle.getPago().getId() == 0) {
                detalle.setPago(pagoManaged);
                log.info("[processDetallesPago] Pago asignado al detalle: (aun sin ID definitivo)");
            }
            // Reatachar asociaciones de otros conceptos (asegurate que este metodo no sobrescriba la asociacion del alumno)
            paymentProcessor.reatacharAsociaciones(detalle, pagoManaged);

            // Verificamos nuevamente el alumno en el detalle
            log.info("[processDetallesPago] Verificacion post reatachar: DetallePago alumno.id={}",
                    (detalle.getAlumno() != null ? detalle.getAlumno().getId() : "null"));

            // Procesar si ya existe un detalle similar
            DetallePago detalleExistente = paymentProcessor.findDetallePagoByCriteria(detalle, alumnoPersistido.getId());
            if (detalleExistente != null) {
                log.info("[processDetallesPago] DetallePago existente encontrado para '{}' (ID: {})",
                        detalle.getDescripcionConcepto(), detalleExistente.getId());
                paymentCalculationServicio.procesarYCalcularDetalle(
                        pagoManaged, detalleExistente, paymentProcessor.obtenerInscripcion(detalle));
                detallesProcesados.add(detalleExistente);
            } else {
                log.info("[processDetallesPago] No se encontro detalle similar para '{}'. Se creara uno nuevo.",
                        detalle.getDescripcionConcepto());
                // Reinicializamos id y version para forzar la persistencia de un nuevo registro.
                detalle.setId(null);
                detalle.setVersion(null);
                paymentCalculationServicio.procesarYCalcularDetalle(
                        pagoManaged, detalle, paymentProcessor.obtenerInscripcion(detalle));
                detallesProcesados.add(detalle);
            }
        }

        // Asignamos cada detalle procesado al pago, forzando la asociacion correcta.
        for (DetallePago det : detallesProcesados) {
            det.setAlumno(alumnoPersistido);
            det.setPago(pagoManaged);
            log.info("[processDetallesPago] Detalle final: descripcionConcepto='{}', alumno.id={}, pago.id={}",
                    det.getDescripcionConcepto(),
                    (det.getAlumno() != null ? det.getAlumno().getId() : "null"),
                    (det.getPago() != null ? det.getPago().getId() : "null"));
        }

        pagoManaged.setDetallePagos(detallesProcesados);
        log.info("[processDetallesPago] Detalles asignados al pago. Cantidad de detalles: {}", detallesProcesados.size());
        paymentProcessor.recalcularTotales(pagoManaged);

        // Aqui se persiste el pago (y, si se tiene configurada cascada, tambien los detalles) de forma unica al final.
        log.info("[processDetallesPago] Se procede a persistir/mergear el pago con todos los detalles asociados.");
        if (pagoManaged.getId() == null) {
            entityManager.persist(pagoManaged);
            log.info("[processDetallesPago] Pago persistido: nuevo ID generado = {}", pagoManaged.getId());
        } else {
            pagoManaged = entityManager.merge(pagoManaged);
            log.info("[processDetallesPago] Pago mergeado: ID = {}", pagoManaged.getId());
        }
        entityManager.flush();

        log.info("[processDetallesPago] Pago final persistido: ID={}, Monto={}, SaldoRestante={}",
                pagoManaged.getId(), pagoManaged.getMonto(), pagoManaged.getSaldoRestante());
        return pagoManaged;
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoRegistroRequest request) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setEstadoPago(request.activo() ? EstadoPago.ACTIVO : EstadoPago.ANULADO);

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        actualizarDetallesPago(pago, request.detallePagos());

        // Solo se actualizan los importes si el pago no esta marcado como HISTORICO.
        if (pago.getEstadoPago() != EstadoPago.HISTORICO) {
            actualizarImportesPagoParcial(pago);
        }

        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        return pagoMapper.toDTO(pago);
    }

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] Actualizando detalles para pago id={}", pago.getId());
        // Mapear detalles existentes por su ID para acceso rapido.
        Map<Long, DetallePago> detallesExistentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));
        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> obtenerODefinirDetallePago(dto, detallesExistentes, pago))
                .toList();
        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        log.info("[actualizarDetallesPago] Detalles actualizados. Recalculando importes...");

        // Recalcular el importe de cada detalle.
        pago.getDetallePagos().forEach(detalle -> {
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={}", detalle.getId());
            detallePagoServicio.calcularImporte(detalle);
        });
    }

    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> detallesExistentes,
                                                   Pago pago) {
        if (dto.id() != null && detallesExistentes.containsKey(dto.id())) {
            // Actualizar detalle existente.
            DetallePago detalle = detallesExistentes.get(dto.id());
            log.info("[obtenerODefinirDetallePago] Actualizando detalle existente id={}", detalle.getId());
            // Actualiza relaciones utilizando el nuevo campo 'descripcionConcepto' si corresponde.
            detalle.setConcepto(dto.conceptoId() != null
                    ? conceptoRepositorio.findById(dto.conceptoId()).orElse(null)
                    : null);
            detalle.setSubConcepto(dto.subConceptoId() != null
                    ? subConceptoRepositorio.findById(dto.subConceptoId()).orElse(null)
                    : null);
            detalle.setValorBase(dto.valorBase());
            detalle.setBonificacion(dto.bonificacionId() != null
                    ? bonificacionRepositorio.findById(dto.bonificacionId()).orElse(null)
                    : null);
            if (dto.recargoId() != null) {
                detalle.setRecargo(recargoRepositorio.findById(dto.recargoId()).orElse(null));
                detalle.setTieneRecargo(true);
            }
            // Se podria actualizar otros campos segun necesidad.
            return detalle;
        } else {
            // Crear un nuevo detalle.
            log.info("[obtenerODefinirDetallePago] Creando nuevo detalle para conceptoId '{}' y subconceptoId '{}'",
                    dto.conceptoId(), dto.subConceptoId());
            DetallePago nuevo = new DetallePago();
            nuevo.setPago(pago);
            nuevo.setConcepto(dto.conceptoId() != null
                    ? conceptoRepositorio.findById(dto.conceptoId()).orElse(null)
                    : null);
            nuevo.setSubConcepto(dto.subConceptoId() != null
                    ? subConceptoRepositorio.findById(dto.subConceptoId()).orElse(null)
                    : null);
            nuevo.setValorBase(dto.valorBase());
            // Asigna el nuevo campo unificado para la descripcion.
            nuevo.setDescripcionConcepto(dto.descripcionConcepto());
            // Si aCobrar esta definido y es mayor a 0, se utiliza; de lo contrario se usa valorBase.
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : 0);
            nuevo.setBonificacion(dto.bonificacionId() != null
                    ? bonificacionRepositorio.findById(dto.bonificacionId()).orElse(null)
                    : null);
            if (dto.recargoId() != null) {
                nuevo.setRecargo(recargoRepositorio.findById(dto.recargoId()).orElse(null));
                nuevo.setTieneRecargo(true);
            }
            return nuevo;
        }
    }

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        // Recuperar al alumno y validar que exista.
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        // Se obtienen los pagos activos (no anulados) y con saldo pendiente.
        List<Pago> pagosPendientes = pagoRepositorio
                .findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;

        // Para cada pago pendiente, sumar el pendiente de cada detalle.
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    detalle.setAlumno(pago.getAlumno());
                    // Se calcula el pendiente usando valorBase y aFavor.
                    double pendiente = detalle.getValorBase();
                    if (pendiente > 0) {
                        // Se obtiene la descripcion del concepto desde la entidad relacionada.
                        String conceptoDescripcion = (detalle.getConcepto() != null && detalle.getConcepto().getDescripcion() != null)
                                ? detalle.getConcepto().getDescripcion()
                                : "N/A";

                        conceptosPendientes.put(conceptoDescripcion,
                                conceptosPendientes.getOrDefault(conceptoDescripcion, 0.0) + pendiente);
                        totalPendiente += pendiente;
                    }
                }
            }
        }

        List<DetalleCobranzaDTO> detalles = conceptosPendientes.entrySet().stream()
                .map(e -> new DetalleCobranzaDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        log.info("[generarCobranzaPorAlumno] Alumno id={} tiene total pendiente: {} con detalles: {}",
                alumnoId, totalPendiente, detalles);

        return new CobranzaDTO(alumno.getId(),
                alumno.getNombre() + " " + alumno.getApellido(),
                totalPendiente,
                detalles);
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                detalle.setAlumno(pago.getAlumno());
                detalle.setRecargo(null);
                detallePagoServicio.calcularImporte(detalle);
                if (!detalle.getCobrado()) {
                    detalle.setImportePendiente(detalle.getImporteInicial());
                }
            }
        }

        assert pago.getDetallePagos() != null;
        double nuevoMonto = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        pago.setMonto(nuevoMonto);

        double sumPagosPrevios = 0;

        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        paymentProcessor.verificarSaldoRestante(pago);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado,
                                             Map<Long, Double> montosPorDetalle, Long metodoPagoId) {

        log.info("[registrarPagoParcial] Iniciando para pagoId={}, montoAbonado={}, metodoPagoId={}",
                pagoId, montoAbonado, metodoPagoId);
        log.info("[registrarPagoParcial] Montos por detalle: {}", montosPorDetalle);

        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        log.info("[registrarPagoParcial] Pago obtenido: id={}, monto={}, saldoRestante={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante());

        MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado."));
        log.info("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}",
                metodo.getId(), metodo.getDescripcion());

        // Se crea el medio de pago
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setMetodo(metodo);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);
        log.info("[registrarPagoParcial] PagoMedio creado y asignado al pago id={}", pago.getId());

        // Actualizacion de cada detalle segun el abono asignado
        for (DetallePago detalle : pago.getDetallePagos()) {
            detalle.setAlumno(pago.getAlumno());
            if (montosPorDetalle.containsKey(detalle.getId())) {
                double abono = montosPorDetalle.get(detalle.getId());
                log.info("[registrarPagoParcial] Procesando detalle id={}. Abono recibido: {}", detalle.getId(), abono);
                if (abono < 0) {
                    throw new IllegalArgumentException("No se permite abonar un monto negativo para el detalle id=" + detalle.getId());
                }
                double pendienteActual = detalle.getImportePendiente();
                double nuevoPendiente = pendienteActual - abono;
                if (nuevoPendiente < 0) {
                    nuevoPendiente = 0;
                }
                log.info("[registrarPagoParcial] Detalle id={} | Pendiente anterior: {} | Nuevo pendiente: {}",
                        detalle.getId(), pendienteActual, nuevoPendiente);
                detalle.setImportePendiente(nuevoPendiente);
                // Se marca como cobrado si el pendiente llega a 0
                if (nuevoPendiente == 0) {
                    detalle.setCobrado(true);
                    log.info("[registrarPagoParcial] Detalle id={} marcado como cobrado", detalle.getId());
                }
            } else {
                log.info("[registrarPagoParcial] Detalle id={} sin abono; pendiente se mantiene: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        actualizarImportesPagoParcial(pago);
        log.info("[registrarPagoParcial] Luego de actualizar importes, saldoRestante={}", pago.getSaldoRestante());

        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago actualizado guardado. Nuevo saldoRestante={}", pago.getSaldoRestante());

        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[registrarPagoParcial] Respuesta generada: {}", response);
        return response;
    }

    // 8. Actualizar importes en pago parcial (invoca verificacion de saldo adicional)
    private void actualizarImportesPagoParcial(Pago pago) {
        log.info("[actualizarImportesPagoParcial] Iniciando actualizacion de importes parciales para pago id={}", pago.getId());
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();
        log.info("[actualizarImportesPagoParcial] Suma de importes pendientes de detalles: {}", totalPendiente);
        pago.setSaldoRestante(totalPendiente);
        paymentProcessor.verificarSaldoRestante(pago);
        log.info("[actualizarImportesPagoParcial] Pago actualizado: saldoRestante={}", pago.getSaldoRestante());
    }

    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        return pagoMapper.toDTO(pago);
    }

    public List<PagoResponse> listarPagos() {
        // Se filtra para devolver unicamente los pagos que NO esten anulados
        return pagoRepositorio.findAll()
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarPago(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        // En lugar de setActivo(false), asignamos EstadoPago.ANULADO
        pago.setEstadoPago(EstadoPago.ANULADO);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByAlumnoIdAndEstadoPagoNotOrderByFechaDesc(alumnoId, EstadoPago.ANULADO)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PagoResponse> listarPagosVencidos() {
        LocalDate hoy = LocalDate.now();
        return pagoRepositorio.findPagosVencidos(hoy, EstadoPago.HISTORICO)
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId);
        return pago != null ? pagoMapper.toDTO(pago) : null;
    }

}
