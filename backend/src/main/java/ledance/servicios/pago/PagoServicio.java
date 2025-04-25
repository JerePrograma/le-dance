package ledance.servicios.pago;

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
import ledance.servicios.pdfs.ReciboStorageService;
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

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    // Servicios para delegar la logica de calculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final ReciboStorageService reciboStorageService;
    private final UsuarioRepositorio usuarioRepositorio;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        DetallePagoMapper detallePagoMapper,
                        RecargoRepositorio recargoRepositorio,
                        BonificacionRepositorio bonificacionRepositorio,
                        DetallePagoServicio detallePagoServicio,
                        PaymentProcessor paymentProcessor,
                        SubConceptoRepositorio subConceptoRepositorio,
                        ConceptoRepositorio conceptoRepositorio, ReciboStorageService reciboStorageService,
                        UsuarioRepositorio usuarioRepositorio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.reciboStorageService = reciboStorageService;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /**
     * Registra un nuevo pago o procesa un abono parcial segun el ultimo pago activo del alumno.
     * Selecciona entre crear un pago nuevo, procesar abono parcial o reutilizar el pago activo.
     */
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Iniciando registro de pago. Payload recibido: {}", request);

        Alumno alumnoPersistido = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalStateException(
                        "Alumno no encontrado para ID: " + request.alumno().id()));
        log.info("[registrarPago] Alumno encontrado: {}", alumnoPersistido);

        // 1) Obtener el ultimo pago activo pendiente
        Pago ultimoPagoActivo = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoPersistido.getId());
        log.info("[registrarPago] Ultimo pago activo obtenido: {}", ultimoPagoActivo);

        Pago pagoFinal;
        if (ultimoPagoActivo == null) {
            // 2a) No hay pago: crear uno nuevo y actualizarle los campos desde el request
            pagoFinal = crearNuevoPago(alumnoPersistido, request);
            log.info("[registrarPago] No se encontro pago activo. Se creo un nuevo pago: {}", pagoFinal);
            actualizarDatosPagoDesdeRequest(pagoFinal, request);
        } else if (esAbonoParcial(ultimoPagoActivo)) {
            // 2b) Hay pago activo y es abono parcial: procesarlo y actualizar solo el nuevo
            pagoFinal = paymentProcessor.procesarAbonoParcial(ultimoPagoActivo, request);
            log.info("[registrarPago] Pago procesado por abono parcial: {}", pagoFinal);
            actualizarDatosPagoDesdeRequest(pagoFinal, request);

        } else {
            // 2c) Se reutiliza el pago activo: NO le cambiamos observaciones ni fecha
            pagoFinal = ultimoPagoActivo;
            log.info("[registrarPago] Se utiliza el pago activo existente: {}", pagoFinal);
        }

        // 3) Asignar metodo de pago y persistir
        paymentProcessor.asignarMetodoYPersistir(pagoFinal, request.metodoPagoId());
        log.info("[registrarPago] Metodo de pago asignado al pago final id={}", pagoFinal.getId());

        // 4) Limpiar asociaciones para la respuesta
        limpiarAsociacionesParaRespuesta(pagoFinal);
        log.info("[registrarPago] Asociaciones limpiadas para respuesta del pago id={}", pagoFinal.getId());

        PagoResponse response = pagoMapper.toDTO(pagoFinal);
        log.info("[registrarPago] Pago registrado con exito. Respuesta final: {}", response);

        // 5) Generar recibo si corresponde
        if (!pagoFinal.getMetodoPago().getDescripcion().equalsIgnoreCase("DEBITO")) {
            reciboStorageService.generarYAlmacenarYEnviarRecibo(pagoFinal);
        }
        return response;
    }

    /**
     * Actualiza unicamente los campos que vienen en el request (observaciones, fechas, usuario).
     * NO se invoca cuando se reutiliza el pago activo.
     */
    private void actualizarDatosPagoDesdeRequest(Pago pago, PagoRegistroRequest request) {
        pago.setObservaciones(request.observaciones());
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fecha());
        Usuario cobrador = usuarioRepositorio.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        pago.setUsuario(cobrador);
    }

    /**
     * Limpia asociaciones innecesarias para la respuesta, por ejemplo, las inscripciones del alumno.
     */
    private void limpiarAsociacionesParaRespuesta(Pago pago) {
        log.info("[limpiarAsociacionesParaRespuesta] Limpiando asociaciones del alumno en el Pago id={}", pago.getId());
        if (pago.getAlumno() != null && pago.getAlumno().getInscripciones() != null) {
            pago.getAlumno().getInscripciones().clear();
            log.info("[limpiarAsociacionesParaRespuesta] Inscripciones del alumno limpiadas.");
        }
    }

    /**
     * Determina si el ultimo pago activo es un abono parcial (saldoRestante > 0).
     */
    private boolean esAbonoParcial(Pago ultimoPagoActivo) {
        log.info("[esAbonoParcial] Evaluando si se trata de un abono parcial para el ultimo pago activo.");
        boolean parcial = (ultimoPagoActivo != null && ultimoPagoActivo.getSaldoRestante() > 0);
        log.info("[esAbonoParcial] Resultado: {}", parcial);
        return parcial;
    }

    /**
     * Crea un nuevo pago a partir del request y procesa los detalles.
     */
    private Pago crearNuevoPago(Alumno alumno, PagoRegistroRequest request) {
        log.info("[crearNuevoPago] Iniciando creacion de nuevo Pago para alumno id={}", alumno.getId());
        Pago nuevoPago = pagoMapper.toEntity(request);
        nuevoPago.setObservaciones(request.observaciones());
        log.info("[crearNuevoPago] Pago mapeado: fecha={}, fechaVencimiento={}, importeInicial={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getImporteInicial());

        if (nuevoPago.getImporteInicial() == null) {
            nuevoPago.setImporteInicial(request.importeInicial());
            log.info("[crearNuevoPago] ImporteInicial asignado desde request: {}", request.importeInicial());
        }

        Optional<MetodoPago> metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId());
        nuevoPago.setMetodoPago(metodoPago.orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado.")));
        nuevoPago.setAlumno(alumno);
        Optional<Usuario> cobrador = usuarioRepositorio.findById(request.usuarioId());
        nuevoPago.setUsuario(cobrador.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado.")));

        // 🔥 SIN COPIAR ACÁ
        List<DetallePago> detallesFront = detallePagoMapper.toEntity(request.detallePagos());

        Pago procesado = paymentProcessor.processDetallesPago(nuevoPago, detallesFront, alumno);

        log.info("[crearNuevoPago] Pago procesado con detalles: {}", procesado);
        return procesado;
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoRegistroRequest request) {
        log.info("[actualizarPago] Iniciando actualizacion de pago ID: {}", id);
        log.info("[actualizarPago] Datos de solicitud: {}", request);

        log.info("[actualizarPago] Buscando pago con ID: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> {
                    log.error("[actualizarPago] Pago no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Pago no encontrado.");
                });
        log.info("[actualizarPago] Pago encontrado - Estado actual: {}", pago.getEstadoPago());

        log.info("[actualizarPago] Actualizando fecha de pago a: {}", request.fecha());
        pago.setFecha(request.fecha());

        log.info("[actualizarPago] Actualizando fecha de vencimiento a: {}", request.fechaVencimiento());
        pago.setFechaVencimiento(request.fechaVencimiento());

        EstadoPago nuevoEstado = request.activo() ? EstadoPago.ACTIVO : EstadoPago.ANULADO;
        log.info("[actualizarPago] Actualizando estado de pago de {} a {}", pago.getEstadoPago(), nuevoEstado);
        pago.setEstadoPago(nuevoEstado);

        if (request.metodoPagoId() != null) {
            log.info("[actualizarPago] Buscando metodo de pago con ID: {}", request.metodoPagoId());
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> {
                        log.error("[actualizarPago] Metodo de pago no encontrado con ID: {}", request.metodoPagoId());
                        return new IllegalArgumentException("Metodo de pago no encontrado.");
                    });
            log.info("[actualizarPago] Asignando metodo de pago: {}", metodo.getDescripcion());
            pago.setMetodoPago(metodo);
        } else {
            log.info("[actualizarPago] No se especifico metodo de pago en la solicitud");
        }

        log.info("[actualizarPago] Actualizando detalles de pago");
        actualizarDetallesPago(pago, request.detallePagos());

        if (pago.getEstadoPago() != EstadoPago.HISTORICO) {
            log.info("[actualizarPago] Actualizando importes del pago (no es HISTORICO)");
            actualizarImportesPagoParcial(pago);
            log.info("[actualizarPago] Importes actualizados - Monto: {}, Pagado: {}, Saldo: {}",
                    pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante());
        } else {
            log.info("[actualizarPago] Pago es HISTORICO, omitiendo actualizacion de importes");
        }

        log.info("[actualizarPago] Verificando saldo restante");
        paymentProcessor.verificarSaldoRestante(pago);
        log.info("[actualizarPago] Estado despues de verificacion: {}", pago.getEstadoPago());

        log.info("[actualizarPago] Guardando cambios en el pago");
        pago.setObservaciones(request.observaciones());
        pagoRepositorio.save(pago);
        log.info("[actualizarPago] Pago guardado exitosamente");

        log.info("[actualizarPago] Generando respuesta DTO");
        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[actualizarPago] Actualizacion completada para pago ID: {}", id);

        return response;
    }

    /**
     * Actualiza los DetallePago del pago basandose en la lista de DetallePagoRegistroRequest.
     */
    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] INICIO - Actualizando detalles para pago id={}", pago.getId());
        log.info("[actualizarDetallesPago] Recibidos {} detalles en la solicitud", detallesDTO.size());

        // Mapea los detalles existentes para actualizacion
        Map<Long, DetallePago> detallesExistentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));
        log.info("[actualizarDetallesPago] Detalles existentes encontrados: {}", detallesExistentes.size());

        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> {
                    log.info("[actualizarDetallesPago] Procesando detalle DTO id={}", dto.id());
                    return obtenerODefinirDetallePago(dto, detallesExistentes, pago);
                })
                .toList();
        log.info("[actualizarDetallesPago] Detalles finales generados: {}", detallesFinales.size());

        // Reemplaza la lista actual de detalles por la actualizada
        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        log.info("[actualizarDetallesPago] Lista actualizada. Total detalles actuales: {}", pago.getDetallePagos().size());

        // Recalcula el importe de cada detalle
        pago.getDetallePagos().forEach(detalle -> {
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={} (Descripcion: {})",
                    detalle.getId(), detalle.getDescripcionConcepto());
            detallePagoServicio.calcularImporte(detalle);
            log.info("[actualizarDetallesPago] Detalle id={} - aCobrar={}, importePendiente={}",
                    detalle.getId(), detalle.getACobrar(), detalle.getImportePendiente());
        });
        log.info("[actualizarDetallesPago] FIN - Proceso completado para pago id={}", pago.getId());
    }

    /**
     * Decide actualizar un DetallePago existente o crear uno nuevo basado en el DTO.
     */
    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> detallesExistentes,
                                                   Pago pago) {
        if (dto.id() != null && detallesExistentes.containsKey(dto.id())) {
            DetallePago detalle = detallesExistentes.get(dto.id());
            log.info("[obtenerODefinirDetallePago] Actualizando detalle existente id={}", detalle.getId());
            detalle.setConcepto(dto.conceptoId() != null
                    ? conceptoRepositorio.findById(dto.conceptoId()).orElse(null)
                    : null);
            detalle.setSubConcepto(dto.subConceptoId() != null
                    ? subConceptoRepositorio.findById(dto.subConceptoId()).orElse(null)
                    : null);
            detalle.setValorBase(dto.valorBase());
            return verificarBonificacion(dto, detalle);
        } else {
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
            nuevo.setDescripcionConcepto(dto.descripcionConcepto());
            // Se utiliza aCobrar (en lugar de ACobrar) siguiendo la convencion
            nuevo.setACobrar((dto.ACobrar() != null && dto.ACobrar() > 0) ? dto.ACobrar() : 0);
            return verificarBonificacion(dto, nuevo);
        }
    }

    /**
     * Verifica y asigna bonificaciones y recargos al DetallePago segun el DTO recibido.
     */
    private DetallePago verificarBonificacion(DetallePagoRegistroRequest dto, DetallePago detalle) {
        detalle.setBonificacion(dto.bonificacionId() != null
                ? bonificacionRepositorio.findById(dto.bonificacionId()).orElse(null)
                : null);
        if (dto.recargoId() != null) {
            detalle.setRecargo(recargoRepositorio.findById(dto.recargoId()).orElse(null));
            detalle.setTieneRecargo(true);
        }
        return detalle;
    }

    /**
     * Genera la cobranza para un alumno a partir de sus pagos activos pendientes.
     */
    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        List<Pago> pagosPendientes = pagoRepositorio
                .findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();
        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    detalle.setFechaRegistro(pago.getFecha());
                    detalle.setAlumno(pago.getAlumno());
                    double pendiente = detalle.getValorBase();
                    if (pendiente > 0) {
                        String conceptoDescripcion = (detalle.getConcepto() != null && detalle.getConcepto().getDescripcion() != null)
                                ? detalle.getConcepto().getDescripcion() : "N/A";
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

    /**
     * Quita manualmente el recargo de cada DetallePago de un pago y recalcula importes.
     */
    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                detalle.setFechaRegistro(pago.getFecha());
                detalle.setConcepto(detalle.getConcepto());
                detalle.setSubConcepto(detalle.getSubConcepto());
                detalle.setAlumno(pago.getAlumno());
                detalle.setRecargo(null);
                detalle.setTieneRecargo(false);
                detallePagoServicio.calcularImporte(detalle);
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

    /**
     * Actualiza los importes del pago basandose en la suma de los importes pendientes de sus detalles.
     */
    private void actualizarImportesPagoParcial(Pago pago) {
        log.info("[actualizarImportesPagoParcial] Actualizando importes para pago id={}", pago.getId());
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();
        log.info("[actualizarImportesPagoParcial] Total pendiente de detalles: {}", totalPendiente);
        pago.setSaldoRestante(totalPendiente);
        paymentProcessor.verificarSaldoRestante(pago);
        log.info("[actualizarImportesPagoParcial] Pago actualizado: saldoRestante={}", pago.getSaldoRestante());
    }

    /**
     * Obtiene un pago por su ID y genera su DTO.
     */
    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        return pagoMapper.toDTO(pago);
    }

    /**
     * Lista todos los pagos que no esten anulados.
     */
    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findAll()
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Elimina un pago marcandolo como ANULADO.
     */
    @Transactional
    public void eliminarPago(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setEstadoPago(EstadoPago.ANULADO);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    /**
     * Lista los pagos de un alumno (excluyendo los anulados) ordenados por fecha descendente.
     */
    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByAlumnoIdAndEstadoPagoNotOrderByFechaDesc(alumnoId, EstadoPago.ANULADO)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista los pagos vencidos.
     */
    public List<PagoResponse> listarPagosVencidos() {
        LocalDate hoy = LocalDate.now();
        return pagoRepositorio.findPagosVencidos(hoy, EstadoPago.HISTORICO)
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el ultimo pago pendiente de un alumno.
     */
    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId);
        return pago != null ? pagoMapper.toDTO(pago) : null;
    }

}
