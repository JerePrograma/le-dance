package ledance.servicios.pago;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
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
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    // Servicios para delegar la logica de cálculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentCalculationServicio paymentCalculationServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final AlumnoMapper alumnoMapper;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        MatriculaServicio matriculaServicio,
                        DetallePagoMapper detallePagoMapper,
                        MensualidadServicio mensualidadServicio,
                        RecargoRepositorio recargoRepositorio,
                        BonificacionRepositorio bonificacionRepositorio,
                        PaymentCalculationServicio paymentCalculationServicio,
                        DetallePagoServicio detallePagoServicio,
                        PaymentProcessor paymentProcessor,
                        SubConceptoRepositorio subConceptoRepositorio,
                        ConceptoRepositorio conceptoRepositorio, AlumnoMapper alumnoMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.paymentCalculationServicio = paymentCalculationServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.alumnoMapper = alumnoMapper;
    }

    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Proceso iniciado para el registro de pago. Payload: {}", request);
        try {
            // 1. Mapear el alumno
            Alumno alumno = alumnoMapper.toEntity(request.alumno());
            log.info("[registrarPago] Alumno mapeado: {}", alumno);

            // 2. Consultar el último pago activo (si existe)
            Pago ultimoPagoActivo = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumno.getId());
            Pago pagoFinal;

            // 3. Verificar si se trata de un abono parcial, es decir:
            //    - Existe un pago activo
            //    - Tiene saldo pendiente mayor a 0
            //    - Se cumple la lógica de elegibilidad para migración a histórico
            boolean esAbonoParcial = (ultimoPagoActivo != null
                    && ultimoPagoActivo.getSaldoRestante() > 0
                    && paymentProcessor.esPagoHistoricoAplicable(ultimoPagoActivo, request));

            if (esAbonoParcial) {
                log.info("[registrarPago] Abono parcial detectado para alumno id={}", alumno.getId());
                // Actualizar el pago activo con los abonos del request
                ultimoPagoActivo = paymentProcessor.actualizarPagoHistoricoConAbonos(ultimoPagoActivo, request);

                // Clonar los detalles pendientes para generar el nuevo pago (solo se clonan los que tienen saldo pendiente)
                Pago nuevoPago = paymentProcessor.clonarDetallesConPendiente(ultimoPagoActivo);
                log.info("[registrarPago] Nuevo pago generado (con detalles pendientes): {}", nuevoPago);
                pagoFinal = nuevoPago;

                // Ahora migrar el pago activo a HISTÓRICO:
                ultimoPagoActivo.setEstadoPago(EstadoPago.HISTORICO);
                ultimoPagoActivo.setSaldoRestante(0.0);
                for (DetallePago dp : ultimoPagoActivo.getDetallePagos()) {
                    dp.setCobrado(true);
                    dp.setImportePendiente(0.0);
                    entityManager.merge(dp);
                }
                entityManager.merge(ultimoPagoActivo);
                entityManager.flush();
            } else {
                log.info("[registrarPago] Registro inicial. Creando un nuevo pago para alumno id={}", alumno.getId());
                pagoFinal = crearNuevoPago(alumno, request);
            }

            // 4. Procesar medios de pago y recalcular totales
            pagoFinal = marcarDetallesConImportePendienteCero(pagoFinal);
            Optional<MetodoPago> metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId());
            if (metodoPago.isPresent()) {
                pagoFinal.setMetodoPago(metodoPago.get());
            }
            paymentProcessor.recalcularTotales(pagoFinal);

            // 5. Limpiar asociaciones innecesarias para la respuesta (por ejemplo, inscripciones)
            if (pagoFinal.getAlumno() != null) {
                pagoFinal.getAlumno().getInscripciones().clear();
            }

            PagoResponse response = pagoMapper.toDTO(pagoFinal);
            log.info("[registrarPago] Pago registrado con éxito. Respuesta final: {}", response);
            return response;
        } catch (Exception e) {
            log.error("[registrarPago] Error en el registro de pago: ", e);
            throw e;
        }
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
        return processDetallesPago(nuevoPago, detallesFront);
    }

    /**
     * Método unificado para procesar los detalles de pago.
     * En el flujo de abono parcial se debe clonar (o filtrar) únicamente los detalles con saldo pendiente.
     *
     * @param pago          el objeto Pago a actualizar
     * @param detallesFront lista de DetallePago provenientes del request
     * @return el objeto Pago actualizado y persistido con totales recalculados.
     */
    private Pago processDetallesPago(Pago pago, List<DetallePago> detallesFront) {
        // Si el pago ya existe en la base de datos se obtiene el objeto gestionado
        Pago pagoManaged = (pago.getId() != null) ? paymentProcessor.loadAndUpdatePago(pago) : pago;
        List<DetallePago> detallesProcesados = new ArrayList<>();

        for (DetallePago detalle : detallesFront) {
            // Reatachar asociaciones comunes y asignar el pago
            paymentProcessor.reatacharAsociaciones(detalle, pagoManaged);
            detalle.setPago(pagoManaged);

            boolean duplicado = false;
            // Verificar duplicados para matrícula o mensualidad
            if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
                duplicado = paymentProcessor.existeDetalleDuplicado(detalle, pagoManaged.getAlumno().getId());
            }
            if (!duplicado && detalle.getMensualidad() != null && detalle.getMensualidad().getId() != null) {
                duplicado = paymentProcessor.existeDetalleDuplicado(detalle, pagoManaged.getAlumno().getId());
            }

            if (duplicado) {
                log.info("[processDetallesPago] Se detectó duplicado para la asociación correspondiente (matrícula o mensualidad).");
                // En este caso se omite el detalle duplicado o se podría actualizar el existente
                continue;
            }

            // Reinicializar el ID para tratarlo como nuevo
            detalle.setId(null);

            // Procesar y calcular el detalle (aplicando cálculo de importe inicial, abono, etc.)
            paymentCalculationServicio.procesarYCalcularDetalle(pagoManaged, detalle, paymentProcessor.obtenerInscripcion(detalle));
            detallesProcesados.add(detalle);
        }

        // Asignar TODOS los detalles procesados al pago (en caso de abono parcial, se espera que paymentProcessor.clonarDetallesConPendiente ya filtre lo no pendiente)
        pagoManaged.setDetallePagos(detallesProcesados);

        // Recalcular totales
        paymentProcessor.recalcularTotales(pagoManaged);

        // Persistir el pago; el merge propagará en cascade los cambios a los DetallePago
        if (pagoManaged.getId() == null) {
            log.info("[processDetallesPago] Persistiendo nuevo pago.");
            entityManager.persist(pagoManaged);
        } else {
            log.info("[processDetallesPago] Actualizando pago existente.");
            pagoManaged = entityManager.merge(pagoManaged);
        }
        entityManager.flush();
        log.info("[processDetallesPago] Pago persistido: id={}, monto={}, saldoRestante={}",
                pagoManaged.getId(), pagoManaged.getMonto(), pagoManaged.getSaldoRestante());
        return pagoManaged;
    }

    private Pago marcarDetallesConImportePendienteCero(Pago pago) {
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                detalle.setCobrado(true);
            }
        });
        return paymentProcessor.verificarSaldoRestante(pago);
    }

    private void actualizarDeudasSiCorrespondiente(Pago pago) {
        log.info("[actualizarDeudasSiCorrespondiente] Evaluando condiciones para actualizar deudas en pago id={}", pago.getId());
        List<TipoDetallePago> tiposDeuda = Arrays.asList(TipoDetallePago.MATRICULA, TipoDetallePago.MENSUALIDAD);
        boolean tieneDetalleDeuda = pago.getDetallePagos().stream().anyMatch(det -> tiposDeuda.contains(det.getTipo()));
        if (pago.getSaldoRestante() == 0 && pago.getAlumno() != null && tieneDetalleDeuda) {
            log.info("[actualizarDeudasSiCorrespondiente] Condición cumplida. Actualizando deudas para alumno id={}", pago.getAlumno().getId());
            actualizarEstadoDeudas(pago.getAlumno().getId(), pago.getFecha());
        } else {
            log.info("[actualizarDeudasSiCorrespondiente] No se cumplen las condiciones para actualizar deudas. Detalles: saldoRestante={}, alumnoId={}, tieneDetalleDeuda={}",
                    pago.getSaldoRestante(),
                    (pago.getAlumno() != null ? pago.getAlumno().getId() : "null"),
                    tieneDetalleDeuda);
        }
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
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        actualizarDetallesPago(pago, request.detallePagos());

        // Solo se actualizan los importes si el pago no está marcado como HISTÓRICO.
        if (pago.getEstadoPago() != EstadoPago.HISTORICO) {
            actualizarImportesPagoParcial(pago);
        }

        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        return pagoMapper.toDTO(pago);
    }

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] Actualizando detalles para pago id={}", pago.getId());
        // Mapear detalles existentes por su ID para acceso rápido.
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
            detalle.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            // Se podría actualizar otros campos según necesidad.
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
            // Asigna el nuevo campo unificado para la descripción.
            nuevo.setDescripcionConcepto(dto.descripcionConcepto());
            // Si aCobrar está definido y es mayor a 0, se utiliza; de lo contrario se usa valorBase.
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : dto.valorBase());
            nuevo.setBonificacion(dto.bonificacionId() != null
                    ? bonificacionRepositorio.findById(dto.bonificacionId()).orElse(null)
                    : null);
            nuevo.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
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
                    // Se calcula el pendiente usando valorBase y aFavor.
                    double pendiente = detalle.getValorBase();
                    if (pendiente > 0) {
                        // Se obtiene la descripción del concepto desde la entidad relacionada.
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

    private void actualizarEstadoDeudas(Long alumnoId, LocalDate fechaPago) {

        Matricula matResp = matriculaServicio.obtenerOMarcarPendienteMatricula(alumnoId, fechaPago.getYear());
        if (matResp != null && !matResp.getPagada()) {
            matriculaServicio.actualizarEstadoMatricula(matResp.getId(),
                    new MatriculaRegistroRequest(alumnoId, matResp.getAnio(), true, fechaPago));
        }

        List<MensualidadResponse> pendientes = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        pendientes.forEach(mens -> {
            if ("PENDIENTE".equalsIgnoreCase(mens.estado()) || "OMITIDO".equalsIgnoreCase(mens.estado())) {
                mensualidadServicio.marcarComoPagada(mens.id(), fechaPago);
            }
        });
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                detalle.setRecargo(null);
                detallePagoServicio.calcularImporte(detalle);
                if (!detalle.getCobrado()) {
                    detalle.setImportePendiente(detalle.getImporteInicial());
                }
            }
        }

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
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        log.info("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}",
                metodo.getId(), metodo.getDescripcion());

        // Se crea el medio de pago
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setMetodo(metodo);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);
        log.info("[registrarPagoParcial] PagoMedio creado y asignado al pago id={}", pago.getId());

        // Actualización de cada detalle según el abono asignado
        for (DetallePago detalle : pago.getDetallePagos()) {
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

    // 8. Actualizar importes en pago parcial (invoca verificación de saldo adicional)
    private void actualizarImportesPagoParcial(Pago pago) {
        log.info("[actualizarImportesPagoParcial] Iniciando actualización de importes parciales para pago id={}", pago.getId());
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();
        log.info("[actualizarImportesPagoParcial] Suma de importes pendientes de detalles: {}", totalPendiente);
        pago.setSaldoRestante(totalPendiente);
        paymentProcessor.verificarSaldoRestante(pago);
        log.info("[actualizarImportesPagoParcial] Pago actualizado: saldoRestante={}", pago.getSaldoRestante());
    }

    private void actualizarMetodoPago(Pago pago, Long metodoPagoId) {
        if (metodoPagoId != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
    }

    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        return pagoMapper.toDTO(pago);
    }

    public List<PagoResponse> listarPagos() {
        // Se filtra para devolver únicamente los pagos que NO estén anulados
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

    @Transactional
    public void generarCuotasParaAlumnosActivos() {
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            Pago nuevoPago = new Pago();
            nuevoPago.setFecha(LocalDate.now());
            nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
            nuevoPago.setMonto(inscripcion.getDisciplina().getValorCuota());
            nuevoPago.setSaldoRestante(inscripcion.getDisciplina().getValorCuota());
            // En lugar de setActivo(true), asignamos EstadoPago.ACTIVO
            nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            paymentProcessor.verificarSaldoRestante(nuevoPago);
            pagoRepositorio.save(nuevoPago);
        }
    }

    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId);
        return pago != null ? pagoMapper.toDTO(pago) : null;
    }

}
