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
                        ConceptoRepositorio conceptoRepositorio, ReciboStorageService reciboStorageService, UsuarioRepositorio usuarioRepositorio, UsuarioRepositorio usuarioRepositorio1) {
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
        this.usuarioRepositorio = usuarioRepositorio1;
    }

    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Iniciando registro de pago. Payload recibido: {}", request);

        Alumno alumnoPersistido = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalStateException("Alumno no encontrado para ID: " + request.alumno().id()));
        log.info("[registrarPago] Alumno encontrado: {}", alumnoPersistido);

        // Intentamos obtener el último pago activo pendiente para el alumno.
        Pago ultimoPagoActivo = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoPersistido.getId());
        log.info("[registrarPago] Último pago activo obtenido: {}", ultimoPagoActivo);

        Pago pagoFinal;
        if (ultimoPagoActivo == null) {
            // No hay pago activo, se crea un único nuevo pago.
            pagoFinal = crearNuevoPago(alumnoPersistido, request);
            log.info("[registrarPago] No se encontró pago activo. Se creó un nuevo pago: {}", pagoFinal);
        } else if (esAbonoParcial(ultimoPagoActivo, request)) {
            // Existe un pago activo y se cumple la lógica para abono parcial.
            pagoFinal = paymentProcessor.procesarAbonoParcial(ultimoPagoActivo, request);
            log.info("[registrarPago] Pago procesado por abono parcial: {}", pagoFinal);
        } else {
            // Existe un pago activo y no se cumple la condición de abono parcial, se reutiliza el existente.
            pagoFinal = ultimoPagoActivo;
            log.info("[registrarPago] Se utiliza el pago activo existente: {}", pagoFinal);
        }
        pagoFinal.setObservaciones(request.observaciones());
        pagoFinal.setFecha(request.fecha());
        Optional<Usuario> usuario = usuarioRepositorio.findById(request.usuarioId());
        Usuario cobrador = usuario.get();
        pagoFinal.setUsuario(cobrador);
        // Asignar el método de pago y persistir el pago final.
        // Se recomienda usar saveAndFlush para asegurarse de que el pago tenga asignado un ID.
        paymentProcessor.asignarMetodoYPersistir(pagoFinal, request.metodoPagoId());
        log.info("[registrarPago] Método de pago asignado al pago final id={}", pagoFinal.getId());

        limpiarAsociacionesParaRespuesta(pagoFinal);
        log.info("[registrarPago] Asociaciones limpiadas para respuesta del pago id={}", pagoFinal.getId());
        PagoResponse response = pagoMapper.toDTO(pagoFinal);
        log.info("[registrarPago] Pago registrado con éxito. Respuesta final: {}", response);
        if (!pagoFinal.getMetodoPago().getDescripcion().equalsIgnoreCase("DEBITO")) {
            reciboStorageService.generarYAlmacenarReciboDesdePagoHistorico(pagoFinal);
        }

        return response;
    }

    /**
     * Limpia asociaciones innecesarias para la respuesta (por ejemplo, las inscripciones del alumno).
     */
    private void limpiarAsociacionesParaRespuesta(Pago pago) {
        log.info("[limpiarAsociacionesParaRespuesta] Limpiando asociaciones del alumno en el Pago id={}", pago.getId());
        if (pago.getAlumno() != null && pago.getAlumno().getInscripciones() != null) {
            pago.getAlumno().getInscripciones().clear();
            log.info("[limpiarAsociacionesParaRespuesta] Inscripciones del alumno limpiadas.");
        }
    }

    /**
     * Valida que exista un pago activo con saldo pendiente y que se cumpla la logica de migracion.
     */
    private boolean esAbonoParcial(Pago ultimoPagoActivo, PagoRegistroRequest request) {
        log.info("[esAbonoParcial] Evaluando si se trata de un abono parcial para el último pago activo.");
        boolean parcial = (ultimoPagoActivo != null
                && ultimoPagoActivo.getSaldoRestante() > 0);
        log.info("[esAbonoParcial] Resultado: {}", parcial);
        return parcial;
    }

    /**
     * Crea un nuevo pago y procesa los detalles enviados en el request.
     * Se asume que en el registro inicial no se debe verificar duplicados.
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
        nuevoPago.setMetodoPago(metodoPago.get());
        nuevoPago.setAlumno(alumno);
        Optional<Usuario> cobrador = usuarioRepositorio.findById(request.usuarioId());
        nuevoPago.setUsuario(cobrador.get());
        List<DetallePago> detallesFront = detallePagoMapper.toEntity(request.detallePagos());
        log.info("[crearNuevoPago] Se obtuvieron {} detalles del request.", detallesFront.size());
        // Aquí se llama a processDetallesPago una sola vez para procesar los detalles y persistir el pago.
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

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] INICIO - Actualizando detalles para pago id={}", pago.getId());
        log.info("[actualizarDetallesPago] Recibidos {} detalles en la solicitud", detallesDTO.size());

        // Mapear detalles existentes por su ID para acceso rapido.
        log.info("[actualizarDetallesPago] Mapeando detalles existentes del pago");
        Map<Long, DetallePago> detallesExistentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));
        log.info("[actualizarDetallesPago] Encontrados {} detalles existentes", detallesExistentes.size());

        log.info("[actualizarDetallesPago] Procesando lista de detalles DTO");
        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> {
                    log.info("[actualizarDetallesPago] Procesando detalle DTO id={}", dto.id());
                    return obtenerODefinirDetallePago(dto, detallesExistentes, pago);
                })
                .toList();
        log.info("[actualizarDetallesPago] Generados {} detalles finales", detallesFinales.size());

        log.info("[actualizarDetallesPago] Limpiando lista actual de detalles del pago");
        pago.getDetallePagos().clear();
        log.info("[actualizarDetallesPago] Añadiendo {} nuevos detalles al pago", detallesFinales.size());
        pago.getDetallePagos().addAll(detallesFinales);
        log.info("[actualizarDetallesPago] Detalles actualizados. Total actual: {}", pago.getDetallePagos().size());

        // Recalcular el importe de cada detalle.
        log.info("[actualizarDetallesPago] Iniciando recálculo de importes para cada detalle");
        pago.getDetallePagos().forEach(detalle -> {
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={} (Descripción: {})",
                    detalle.getId(), detalle.getDescripcionConcepto());
            detallePagoServicio.calcularImporte(detalle);
            log.info("[actualizarDetallesPago] Detalle id={} - Importe recalculado: aCobrar={}, pendiente={}",
                    detalle.getId(), detalle.getaCobrar(), detalle.getImportePendiente());
        });

        log.info("[actualizarDetallesPago] FIN - Proceso completado para pago id={}", pago.getId());
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
            return verificarBonificacion(dto, detalle);
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
            return verificarBonificacion(dto, nuevo);
        }
    }

    private DetallePago verificarBonificacion(DetallePagoRegistroRequest dto, DetallePago nuevo) {
        nuevo.setBonificacion(dto.bonificacionId() != null
                ? bonificacionRepositorio.findById(dto.bonificacionId()).orElse(null)
                : null);
        if (dto.recargoId() != null) {
            nuevo.setRecargo(recargoRepositorio.findById(dto.recargoId()).orElse(null));
            nuevo.setTieneRecargo(true);
        }
        return nuevo;
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
                    detalle.setFechaRegistro(pago.getFecha());
                    detalle.setAlumno(pago.getAlumno());
                    detalle.setConcepto(detalle.getConcepto());
                    detalle.setSubConcepto(detalle.getSubConcepto());
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
                detalle.setFechaRegistro(pago.getFecha());
                detalle.setConcepto(detalle.getConcepto());
                detalle.setSubConcepto(detalle.getSubConcepto());
                detalle.setAlumno(pago.getAlumno());
                detalle.setRecargo(null);
                detalle.setTieneRecargo(false);
                detalle.setImportePendiente(detalle.getImporteInicial());
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

    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado,
                                             Map<Long, Double> montosPorDetalle, Long metodoPagoId) {

        log.info("[registrarPagoParcial] Iniciando para pagoId={}, montoAbonado={}, metodoPagoId={}",
                pagoId, montoAbonado, metodoPagoId);
        log.info("[registrarPagoParcial] Montos por detalle: {}", montosPorDetalle);

        log.info("[registrarPagoParcial] Buscando pago con id={}", pagoId);
        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        log.info("[registrarPagoParcial] Pago obtenido: id={}, monto={}, saldoRestante={}, estado={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante(), pago.getEstadoPago());

        log.info("[registrarPagoParcial] Buscando método de pago con id={}", metodoPagoId);
        MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado."));
        log.info("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}, activo={}",
                metodo.getId(), metodo.getDescripcion(), metodo.getActivo());

        // Se crea el medio de pago
        log.info("[registrarPagoParcial] Creando nuevo PagoMedio");
        PagoMedio pagoMedio = new PagoMedio();
        log.info("[registrarPagoParcial] Asignando monto al PagoMedio: {}", montoAbonado);
        pagoMedio.setMonto(montoAbonado);
        log.info("[registrarPagoParcial] Asignando método de pago al PagoMedio");
        pagoMedio.setMetodo(metodo);
        log.info("[registrarPagoParcial] Asignando pago al PagoMedio");
        pagoMedio.setPago(pago);

        log.info("[registrarPagoParcial] Agregando PagoMedio a la lista de medios de pago");
        pago.getPagoMedios().add(pagoMedio);
        log.info("[registrarPagoParcial] PagoMedio creado y asignado al pago id={}. Total medios de pago: {}",
                pago.getId(), pago.getPagoMedios().size());

        // Actualizacion de cada detalle segun el abono asignado
        log.info("[registrarPagoParcial] Procesando {} detalles de pago", pago.getDetallePagos().size());
        for (DetallePago detalle : pago.getDetallePagos()) {
            detalle.setFechaRegistro(pago.getFecha());
            detalle.setConcepto(detalle.getConcepto());
            detalle.setSubConcepto(detalle.getSubConcepto());
            log.info("[registrarPagoParcial] Procesando detalle id={}", detalle.getId());
            log.info("[registrarPagoParcial] Asignando alumno al detalle: alumnoId={}", pago.getAlumno().getId());
            detalle.setAlumno(pago.getAlumno());

            if (montosPorDetalle.containsKey(detalle.getId())) {
                double abono = montosPorDetalle.get(detalle.getId());
                log.info("[registrarPagoParcial] Detalle id={}. Abono recibido: {}", detalle.getId(), abono);

                if (abono < 0) {
                    log.error("[registrarPagoParcial] Error: Abono negativo para detalle id={}", detalle.getId());
                    throw new IllegalArgumentException("No se permite abonar un monto negativo para el detalle id=" + detalle.getId());
                }

                double pendienteActual = detalle.getImportePendiente();
                log.info("[registrarPagoParcial] Detalle id={}. Pendiente actual: {}", detalle.getId(), pendienteActual);

                double nuevoPendiente = pendienteActual - abono;
                log.info("[registrarPagoParcial] Detalle id={}. Nuevo pendiente calculado: {}", detalle.getId(), nuevoPendiente);

                if (nuevoPendiente < 0) {
                    log.info("[registrarPagoParcial] Ajustando nuevo pendiente a 0 (era negativo)");
                    nuevoPendiente = 0;
                }

                log.info("[registrarPagoParcial] Detalle id={} | Pendiente anterior: {} | Nuevo pendiente: {}",
                        detalle.getId(), pendienteActual, nuevoPendiente);
                detalle.setImportePendiente(nuevoPendiente);

                // Se marca como cobrado si el pendiente llega a 0
                if (nuevoPendiente == 0) {
                    log.info("[registrarPagoParcial] Marcando detalle id={} como cobrado", detalle.getId());
                    detalle.setCobrado(true);
                    log.info("[registrarPagoParcial] Detalle id={} marcado como cobrado", detalle.getId());
                }
            } else {
                log.info("[registrarPagoParcial] Detalle id={} sin abono; pendiente se mantiene: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        log.info("[registrarPagoParcial] Actualizando importes del pago");
        actualizarImportesPagoParcial(pago);
        log.info("[registrarPagoParcial] Luego de actualizar importes, monto={}, montoPagado={}, saldoRestante={}, estado={}",
                pago.getMonto(), pago.getMontoPagado(), pago.getSaldoRestante(), pago.getEstadoPago());

        log.info("[registrarPagoParcial] Verificando saldo restante con paymentProcessor");
        paymentProcessor.verificarSaldoRestante(pago);

        log.info("[registrarPagoParcial] Guardando pago actualizado");
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago actualizado guardado. Nuevo estado: {}", pago.getEstadoPago());

        log.info("[registrarPagoParcial] Generando respuesta DTO");
        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[registrarPagoParcial] Respuesta generada con éxito. Pago id={}, estado={}",
                response.id(), response.estadoPago());

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
