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
     * Registra un nuevo pago o procesa un abono parcial según el último pago activo del alumno.
     * Selecciona entre crear un pago nuevo, procesar abono parcial o reutilizar el pago activo.
     */
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Iniciando registro de pago. Payload recibido: {}", request);

        Alumno alumnoPersistido = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalStateException("Alumno no encontrado para ID: " + request.alumno().id()));
        log.info("[registrarPago] Alumno encontrado: {}", alumnoPersistido);

        // Obtiene el último pago activo pendiente del alumno
        Pago ultimoPagoActivo = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoPersistido.getId());
        log.info("[registrarPago] Último pago activo obtenido: {}", ultimoPagoActivo);

        Pago pagoFinal;
        if (ultimoPagoActivo == null) {
            // No hay pago activo: se crea un pago nuevo.
            pagoFinal = crearNuevoPago(alumnoPersistido, request);
            log.info("[registrarPago] No se encontró pago activo. Se creó un nuevo pago: {}", pagoFinal);
        } else if (esAbonoParcial(ultimoPagoActivo)) {
            // Si se cumple la condición para abono parcial, procesarlo.
            pagoFinal = paymentProcessor.procesarAbonoParcial(ultimoPagoActivo, request);
            log.info("[registrarPago] Pago procesado por abono parcial: {}", pagoFinal);
        } else {
            // Se reutiliza el pago activo existente.
            pagoFinal = ultimoPagoActivo;
            log.info("[registrarPago] Se utiliza el pago activo existente: {}", pagoFinal);
        }

        // Actualiza campos comunes del pago según la solicitud.
        actualizarDatosPagoDesdeRequest(pagoFinal, request);

        // Asigna el método de pago y persiste el pago final (usando saveAndFlush para garantizar el ID)
        paymentProcessor.asignarMetodoYPersistir(pagoFinal, request.metodoPagoId());
        log.info("[registrarPago] Método de pago asignado al pago final id={}", pagoFinal.getId());

        limpiarAsociacionesParaRespuesta(pagoFinal);
        log.info("[registrarPago] Asociaciones limpiadas para respuesta del pago id={}", pagoFinal.getId());
        PagoResponse response = pagoMapper.toDTO(pagoFinal);
        log.info("[registrarPago] Pago registrado con éxito. Respuesta final: {}", response);

        // Genera recibo, excepto en caso de método DEBITO
        if (!pagoFinal.getMetodoPago().getDescripcion().equalsIgnoreCase("DEBITO")) {
            reciboStorageService.generarYAlmacenarYEnviarRecibo(pagoFinal);
        }
        return response;
    }

    /**
     * Actualiza datos comunes del pago (observaciones, fechas y usuario) a partir del request.
     */
    private void actualizarDatosPagoDesdeRequest(Pago pago, PagoRegistroRequest request) {
        pago.setObservaciones(request.observaciones());
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fecha());
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findById(request.usuarioId());
        Usuario cobrador = usuarioOpt.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
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
     * Determina si el último pago activo es un abono parcial (saldoRestante > 0).
     */
    private boolean esAbonoParcial(Pago ultimoPagoActivo) {
        log.info("[esAbonoParcial] Evaluando si se trata de un abono parcial para el último pago activo.");
        boolean parcial = (ultimoPagoActivo != null && ultimoPagoActivo.getSaldoRestante() > 0);
        log.info("[esAbonoParcial] Resultado: {}", parcial);
        return parcial;
    }

    /**
     * Crea un nuevo pago a partir del request y procesa los detalles.
     */
    private Pago crearNuevoPago(Alumno alumno, PagoRegistroRequest request) {
        log.info("[crearNuevoPago] Iniciando creación de nuevo Pago para alumno id={}", alumno.getId());
        Pago nuevoPago = pagoMapper.toEntity(request);
        nuevoPago.setObservaciones(request.observaciones());
        log.info("[crearNuevoPago] Pago mapeado: fecha={}, fechaVencimiento={}, importeInicial={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getImporteInicial());
        if (nuevoPago.getImporteInicial() == null) {
            nuevoPago.setImporteInicial(request.importeInicial());
            log.info("[crearNuevoPago] ImporteInicial asignado desde request: {}", request.importeInicial());
        }
        Optional<MetodoPago> metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId());
        nuevoPago.setMetodoPago(metodoPago.orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado.")));
        nuevoPago.setAlumno(alumno);
        Optional<Usuario> cobrador = usuarioRepositorio.findById(request.usuarioId());
        nuevoPago.setUsuario(cobrador.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado.")));
        List<DetallePago> detallesFront = detallePagoMapper.toEntity(request.detallePagos());
        log.info("[crearNuevoPago] Se obtuvieron {} detalles del request.", detallesFront.size());
        // Procesa los detalles y persiste el pago en una única llamada
        Pago procesado = paymentProcessor.processDetallesPago(nuevoPago, detallesFront, alumno);
        log.info("[crearNuevoPago] Pago procesado con detalles: {}", procesado);
        return procesado;
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoRegistroRequest request) {
        log.info("[actualizarPago] Iniciando actualización de pago ID: {}", id);
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
            log.info("[actualizarPago] Buscando método de pago con ID: {}", request.metodoPagoId());
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> {
                        log.error("[actualizarPago] Método de pago no encontrado con ID: {}", request.metodoPagoId());
                        return new IllegalArgumentException("Metodo de pago no encontrado.");
                    });
            log.info("[actualizarPago] Asignando método de pago: {}", metodo.getDescripcion());
            pago.setMetodoPago(metodo);
        } else {
            log.info("[actualizarPago] No se especificó método de pago en la solicitud");
        }

        log.info("[actualizarPago] Actualizando detalles de pago");
        actualizarDetallesPago(pago, request.detallePagos());

        if (pago.getEstadoPago() != EstadoPago.HISTORICO) {
            log.info("[actualizarPago] Actualizando importes del pago (no es HISTORICO)");
            actualizarImportesPagoParcial(pago);
            log.info("[actualizarPago] Importes actualizados - Monto: {}, Pagado: {}, Saldo: {}",
                    pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante());
        } else {
            log.info("[actualizarPago] Pago es HISTORICO, omitiendo actualización de importes");
        }

        log.info("[actualizarPago] Verificando saldo restante");
        paymentProcessor.verificarSaldoRestante(pago);
        log.info("[actualizarPago] Estado después de verificación: {}", pago.getEstadoPago());

        log.info("[actualizarPago] Guardando cambios en el pago");
        pago.setObservaciones(request.observaciones());
        pagoRepositorio.save(pago);
        log.info("[actualizarPago] Pago guardado exitosamente");

        log.info("[actualizarPago] Generando respuesta DTO");
        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[actualizarPago] Actualización completada para pago ID: {}", id);

        return response;
    }

    /**
     * Actualiza los DetallePago del pago basándose en la lista de DetallePagoRegistroRequest.
     */
    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] INICIO - Actualizando detalles para pago id={}", pago.getId());
        log.info("[actualizarDetallesPago] Recibidos {} detalles en la solicitud", detallesDTO.size());

        // Mapea los detalles existentes para actualización
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
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={} (Descripción: {})",
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
            // Se utiliza aCobrar (en lugar de ACobrar) siguiendo la convención
            nuevo.setACobrar((dto.ACobrar() != null && dto.ACobrar() > 0) ? dto.ACobrar() : 0);
            return verificarBonificacion(dto, nuevo);
        }
    }

    /**
     * Verifica y asigna bonificaciones y recargos al DetallePago según el DTO recibido.
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
     * Procesa el registro de un pago parcial, distribuyendo el abono entre los detalles del pago.
     */
    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado,
                                             Map<Long, Double> montosPorDetalle, Long metodoPagoId) {
        log.info("[registrarPagoParcial] Iniciando para pagoId={}, montoAbonado={}, metodoPagoId={}",
                pagoId, montoAbonado, metodoPagoId);
        log.info("[registrarPagoParcial] Montos por detalle: {}", montosPorDetalle);

        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        log.info("[registrarPagoParcial] Pago obtenido: id={}, monto={}, saldoRestante={}, estado={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante(), pago.getEstadoPago());

        Optional<MetodoPago> metodoOpt = metodoPagoRepositorio.findById(metodoPagoId);
        MetodoPago metodo = metodoOpt.orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        log.info("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}, activo={}",
                metodo.getId(), metodo.getDescripcion(), metodo.getActivo());

        // Crear y asignar el medio de pago
        PagoMedio pagoMedio = crearPagoMedio(montoAbonado, metodo, pago);
        pago.getPagoMedios().add(pagoMedio);
        log.info("[registrarPagoParcial] PagoMedio creado y asignado. Total medios de pago: {}",
                pago.getPagoMedios().size());

        // Aplicar el abono a cada DetallePago usando el mapa de montos
        aplicarAbonoADetalles(pago, montosPorDetalle);

        // Actualizar totales del pago basándose en los detalles actualizados
        actualizarImportesPagoParcial(pago);
        log.info("[registrarPagoParcial] Tras actualización, monto={}, montoPagado={}, saldoRestante={}, estado={}",
                pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante(), pago.getEstadoPago());

        paymentProcessor.verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago actualizado guardado. Nuevo estado: {}", pago.getEstadoPago());

        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[registrarPagoParcial] Respuesta generada con éxito. Pago id={}, estado={}",
                response.id(), response.estadoPago());
        return response;
    }

    /**
     * Crea un objeto PagoMedio con el monto, método y asociación al pago.
     */
    private PagoMedio crearPagoMedio(Double montoAbonado, MetodoPago metodo, Pago pago) {
        log.info("[crearPagoMedio] Creando nuevo PagoMedio para pago id={}", pago.getId());
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setMetodo(metodo);
        pagoMedio.setPago(pago);
        log.info("[crearPagoMedio] PagoMedio creado con monto: {}", montoAbonado);
        return pagoMedio;
    }

    /**
     * Aplica el abono a cada detalle del pago basado en el mapa de montos.
     */
    private void aplicarAbonoADetalles(Pago pago, Map<Long, Double> montosPorDetalle) {
        log.info("[aplicarAbonoADetalles] Procesando {} detalles para pago id={}",
                pago.getDetallePagos().size(), pago.getId());
        for (DetallePago detalle : pago.getDetallePagos()) {
            // Actualiza datos básicos para el detalle
            detalle.setFechaRegistro(pago.getFecha());
            detalle.setAlumno(pago.getAlumno());
            log.info("[aplicarAbonoADetalles] Procesando detalle id={}", detalle.getId());
            if (montosPorDetalle.containsKey(detalle.getId())) {
                double abono = montosPorDetalle.get(detalle.getId());
                if (abono < 0) {
                    log.error("[aplicarAbonoADetalles] Abono negativo para detalle id={}", detalle.getId());
                    throw new IllegalArgumentException("No se permite abonar un monto negativo para el detalle id=" + detalle.getId());
                }
                double pendienteActual = detalle.getImportePendiente() == null ? 0.0 : detalle.getImportePendiente();
                double nuevoPendiente = pendienteActual - abono;
                if (nuevoPendiente < 0) {
                    log.info("[aplicarAbonoADetalles] Ajustando nuevo pendiente a 0 para detalle id={}", detalle.getId());
                    nuevoPendiente = 0;
                }
                log.info("[aplicarAbonoADetalles] Detalle id={} | Pendiente anterior: {} | Nuevo pendiente: {}",
                        detalle.getId(), pendienteActual, nuevoPendiente);
                detalle.setImportePendiente(nuevoPendiente);
                if (nuevoPendiente == 0) {
                    detalle.setCobrado(true);
                    log.info("[aplicarAbonoADetalles] Detalle id={} marcado como cobrado", detalle.getId());
                }
            } else {
                log.info("[aplicarAbonoADetalles] Detalle id={} sin abono; pendiente se mantiene: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }
    }

    /**
     * Actualiza los importes del pago basándose en la suma de los importes pendientes de sus detalles.
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
     * Lista todos los pagos que no estén anulados.
     */
    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findAll()
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Elimina un pago marcándolo como ANULADO.
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
     * Obtiene el último pago pendiente de un alumno.
     */
    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId);
        return pago != null ? pagoMapper.toDTO(pago) : null;
    }

}
