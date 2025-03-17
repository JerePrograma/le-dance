package ledance.servicios.pago;

import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoMedioResponse;
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
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;
    private final InscripcionMapper inscripcionMapper;

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
                        DetallePagoRepositorio detallePagoRepositorio,
                        DetallePagoServicio detallePagoServicio,
                        PaymentProcessor paymentProcessor,
                        DetallePagoMapper detallePagoMapper1,
                        PagoMedioMapper pagoMedioMapper,
                        InscripcionMapper inscripcionMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.pagoMedioMapper = pagoMedioMapper;
        this.inscripcionMapper = inscripcionMapper;
    }

    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[registrarPago] Iniciando registro de pago para inscripción: {}",
                request.inscripcion() != null ? request.inscripcion().id() : "N/A");

        try {
            // Paso 1: Crear el pago (histórico o nuevo) según la inscripción.
            log.info("[registrarPago] Paso 1: Creando pago según inscripción...");
            Pago pagoFinal = paymentProcessor.crearPagoSegunInscripcion(request);
            log.info("[registrarPago] Pago generado: id={}, monto={}, saldoRestante={}",
                    pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());

            // Paso 2: Marcar como cobrados los detalles con importe pendiente = 0.
            log.info("[registrarPago] Paso 2: Marcando detalles con importe pendiente = 0");
            marcarDetallesCobradosSiImporteEsCero(pagoFinal);

            // Paso 3: Registrar los medios de pago (si existen).
            log.info("[registrarPago] Paso 3: Registrando medios de pago...");
            registrarMediosDePago(pagoFinal, request.pagoMedios());

            // Paso 4: Actualizar importes (recalcular descuentos, recargos y saldo).
            log.info("[registrarPago] Paso 4: Actualizando importes del pago...");
            paymentProcessor.actualizarImportesPago(pagoFinal);
            verificarSaldoRestante(pagoFinal);
            log.info("[registrarPago] Importes actualizados: montoPagado={}, saldoRestante={}",
                    pagoFinal.getMontoPagado(), pagoFinal.getSaldoRestante());
            // Paso 5: Persistir el pago.
            log.info("[registrarPago] Paso 5: Persistiendo pago en la base de datos...");
            pagoRepositorio.save(pagoFinal);
            log.info("[registrarPago] Pago guardado: id={}, saldoRestante={}",
                    pagoFinal.getId(), pagoFinal.getSaldoRestante());

            // Paso 6: Actualizar deudas (si aplica).
            log.info("[registrarPago] Paso 6: Verificando detalles de deuda (MATRICULA/MENSUALIDAD)...");
            List<TipoDetallePago> tiposDeuda = Arrays.asList(TipoDetallePago.MATRICULA, TipoDetallePago.MENSUALIDAD);
            boolean tieneDetalleDeuda = pagoFinal.getDetallePagos().stream()
                    .peek(det -> log.info("[registrarPago] Detalle: id={}, tipo={}", det.getId(), det.getTipo()))
                    .anyMatch(det -> tiposDeuda.contains(det.getTipo()));
            log.info("[registrarPago] Resultado - Tiene detalle de deuda: {}", tieneDetalleDeuda);

            if (pagoFinal.getSaldoRestante() == 0
                    && pagoFinal.getAlumno() != null
                    && pagoFinal.getInscripcion() != null
                    && tieneDetalleDeuda) {
                log.info("[registrarPago] Condición para actualizar deudas cumplida: saldoRestante=0, alumno id={}, inscripción id={}",
                        pagoFinal.getAlumno().getId(), pagoFinal.getInscripcion().getId());
                actualizarEstadoDeudas(pagoFinal.getAlumno().getId(), pagoFinal.getFecha());
                log.info("[registrarPago] Deudas actualizadas para alumno id={}", pagoFinal.getAlumno().getId());
            } else {
                log.info("[registrarPago] Condición para actualizar deudas no cumplida. Detalles: saldoRestante={}, alumno={}, inscripción={}, tieneDetalleDeuda={}",
                        pagoFinal.getSaldoRestante(),
                        (pagoFinal.getAlumno() != null ? pagoFinal.getAlumno().getId() : "null"),
                        (pagoFinal.getInscripcion() != null ? pagoFinal.getInscripcion().getId() : "null"),
                        tieneDetalleDeuda);
            }

            // Paso 7: Mapear la entidad Pago a DTO para la respuesta.
            log.info("[registrarPago] Paso 7: Mapeando entidad a DTO...");
            PagoResponse response = pagoMapper.toDTO(pagoFinal);
            log.info("[registrarPago] Respuesta generada: {}", response);

            return response;
        } catch (Exception e) {
            log.error("[registrarPago] Error durante el registro de pago: ", e);
            throw e;
        }
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoRegistroRequest request) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setEstadoPago(request.activo() ? EstadoPago.ACTIVO : EstadoPago.ANULADO);

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        actualizarDetallesPago(pago, request.detallePagos());

        // Solo se actualizan los importes si el pago no está marcado como HISTÓRICO.
        if (pago.getEstadoPago() != EstadoPago.HISTORICO) {
            paymentProcessor.actualizarImportesPago(pago);
        }

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        if (pago.getSaldoRestante() == 0) {
            actualizarEstadoDeudas(pago.getAlumno().getId(), pago.getFecha());
        }

        return pagoMapper.toDTO(pago);
    }

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] Actualizando detalles para pago id={}", pago.getId());
        // Mapeo de detalles existentes por su ID
        Map<Long, DetallePago> existentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));

        // Para cada detalle del DTO se obtiene o define el detalle correspondiente
        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> obtenerODefinirDetallePago(dto, existentes, pago))
                .toList();

        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        log.info("[actualizarDetallesPago] Detalles actualizados. Recalculando importes para cada detalle.");

        // Recalcular el importe de cada detalle (se preserva el aCobrar acumulado)
        pago.getDetallePagos().forEach(detalle -> {
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={}", detalle.getId());
            detallePagoServicio.calcularImporte(detalle);
        });
    }

    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> existentes,
                                                   Pago pago) {
        if (dto.id() != null && existentes.containsKey(dto.id())) {
            // Se actualiza un detalle existente.
            DetallePago detalle = existentes.get(dto.id());
            log.info("[obtenerODefinirDetallePago] Actualizando detalle existente id={}", detalle.getId());
            detalle.setConcepto(dto.concepto());
            // Se usa el nuevo nombre: montoOriginal en lugar de valorBase.
            detalle.setMontoOriginal(dto.montoOriginal());
            // Se preserva el acumulado de aCobrar; no se sobrescribe.
            // Actualizar otros campos:
            detalle.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
            detalle.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            return detalle;
        } else {
            // Se crea un nuevo detalle.
            log.info("[obtenerODefinirDetallePago] Creando nuevo detalle para concepto '{}'", dto.concepto());
            DetallePago nuevo = new DetallePago();
            nuevo.setPago(pago);
            nuevo.setConcepto(dto.concepto());
            // Se asigna el montoOriginal (antes valorBase) del DTO.
            nuevo.setMontoOriginal(dto.montoOriginal());
            // Si aCobrar viene definido y mayor a 0, se usa; de lo contrario se asigna el montoOriginal.
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : dto.montoOriginal());
            nuevo.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
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
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;
        // Para cada pago pendiente, sumar el pendiente de cada detalle.
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    // Se calcula el pendiente usando montoOriginal y aFavor.
                    double pendiente = detalle.getMontoOriginal() -
                            (detalle.getAFavor() != null ? detalle.getAFavor() : 0.0);
                    if (pendiente > 0) {
                        String concepto = detalle.getConcepto();
                        conceptosPendientes.put(concepto, conceptosPendientes.getOrDefault(concepto, 0.0) + pendiente);
                        totalPendiente += pendiente;
                    }
                }
            }
        }
        List<DetalleCobranzaDTO> detalles = conceptosPendientes.entrySet().stream()
                .map(e -> new DetalleCobranzaDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        log.info("[generarCobranzaPorAlumno] Alumno id={} tiene total pendiente: {} con detalles: {}", alumnoId, totalPendiente, detalles);
        return new CobranzaDTO(alumno.getId(), alumno.getNombre() + " " + alumno.getApellido(), totalPendiente, detalles);
    }

    public PagoResponse obtenerUltimoPagoPendiente(Long alumnoId) {
        // Se asume que el repositorio ha sido actualizado para filtrar por EstadoPago.ACTIVO
        Optional<Pago> pagoOpt = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0);
        return pagoOpt.map(pagoMapper::toDTO).orElse(null);
    }

    private void actualizarEstadoDeudas(Long alumnoId, LocalDate fechaPago) {

        MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matResp != null && !matResp.pagada()) {
            matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                    new MatriculaModificacionRequest(matResp.anio(), true, fechaPago));
        }

        List<MensualidadResponse> pendientes = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        pendientes.forEach(mens -> {
            if ("PENDIENTE".equalsIgnoreCase(mens.estado()) || "OMITIDO".equalsIgnoreCase(mens.estado())) {
                mensualidadServicio.marcarComoPagada(mens.id(), fechaPago);
            }
        });
    }

    private void registrarMediosDePago(Pago pago, List<PagoMedioRegistroRequest> mediosPago) {
        if (mediosPago == null || mediosPago.isEmpty()) {
            return;
        }

        List<PagoMedio> medios = mediosPago.stream()
                .map(request -> {
                    MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                            .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado con ID: " + request.metodoPagoId()));
                    PagoMedio pagoMedio = new PagoMedio();
                    pagoMedio.setMonto(request.montoAbonado());
                    pagoMedio.setMetodo(metodo);
                    pagoMedio.setPago(pago);
                    return pagoMedio;
                })
                .toList();

        pago.getPagoMedios().addAll(medios);
    }

    private void marcarDetallesCobradosSiImporteEsCero(Pago pago) {
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                detalle.setCobrado(true);
            }
        });
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setRecargoAplicado(false);

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
        if (pago.getInscripcion() != null) {
            sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(pago.getInscripcion().getId())
                    .stream()
                    .mapToDouble(Pago::getMonto)
                    .sum();
        }
        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        verificarSaldoRestante(pago);
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

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago actualizado guardado. Nuevo saldoRestante={}", pago.getSaldoRestante());

        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[registrarPagoParcial] Respuesta generada: {}", response);
        return response;
    }

    private void actualizarImportesPagoParcial(Pago pago) {
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        log.info("[actualizarImportesPagoParcial] Suma de importes pendientes de detalles: {}", totalPendiente);
        pago.setSaldoRestante(totalPendiente);
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[actualizarImportesPagoParcial] Pago actualizado, saldoRestante={}", pago.getSaldoRestante());
    }

    @Transactional
    public PagoResponse obtenerUltimoPagoPendientePorAlumno(Long alumnoId) {
        // Se busca el último pago ACTIVO con saldo pendiente para el alumno.
        Pago pago = pagoRepositorio
                .findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago pendiente para el alumno con ID: " + alumnoId));

        log.info("[obtenerUltimoPagoPendientePorAlumno] Pago pendiente obtenido: id={}, saldoRestante={}", pago.getId(), pago.getSaldoRestante());

        // Filtrar los detalles que aún no se han cobrado.
        List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                .filter(detalle -> !detalle.getCobrado())
                .map(detallePagoMapper::toDTO)
                .toList();
        log.info("[obtenerUltimoPagoPendientePorAlumno] Se obtuvieron {} detalle(s) pendientes", detallesFiltrados.size());

        // Mapear los medios de pago.
        List<PagoMedioResponse> mediosPago = pago.getPagoMedios().stream()
                .map(pagoMedioMapper::toDTO)
                .toList();

        // Construir y retornar el DTO de respuesta.
        return new PagoResponse(
                pago.getId(),
                pago.getFecha(),
                pago.getFechaVencimiento(),
                pago.getMonto(),                  // Monto total de este pago.
                pago.getMontoBasePago(),          // Nuevo campo: monto original asignado a este pago.
                pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : "",
                pago.getRecargoAplicado(),
                pago.getBonificacionAplicada(),
                pago.getSaldoRestante(),
                pago.getSaldoAFavor(),
                pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO), // 'activo' se determina según el estado.
                pago.getEstadoPago().name(),       // Estado en formato String.
                pago.getInscripcion() != null ? inscripcionMapper.toDTO(pago.getInscripcion()) : null,
                pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                pago.getObservaciones(),
                detallesFiltrados,
                mediosPago,
                pago.getTipoPago().toString()
        );
    }

    @Transactional
    public DeudasPendientesResponse listarDeudasPendientesPorAlumno(Long alumnoId) {
        // Se obtienen los pagos ACTIVOS con saldo pendiente para el alumno.
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();
        log.info("[listarDeudasPendientesPorAlumno] Se encontraron {} pago(s) pendientes para el alumno id={}", pagosPendientes.size(), alumnoId);

        // Para cada pago pendiente, se mapean los detalles pendientes y se construye el DTO de pago.
        List<PagoResponse> pagosPendientesDTO = pagosPendientes.stream()
                .map(pago -> {
                    List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                            .filter(detalle -> !detalle.getCobrado())
                            .map(detallePagoMapper::toDTO)
                            .toList();
                    log.info("[listarDeudasPendientesPorAlumno] Para el pago id={} se encontraron {} detalle(s) pendientes", pago.getId(), detallesFiltrados.size());
                    return new PagoResponse(
                            pago.getId(),
                            pago.getFecha(),
                            pago.getFechaVencimiento(),
                            pago.getMonto(),
                            pago.getMontoBasePago(), // Nuevo campo: monto base del pago.
                            pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : "",
                            pago.getRecargoAplicado(),
                            pago.getBonificacionAplicada(),
                            pago.getSaldoRestante(),
                            pago.getSaldoAFavor(),
                            pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO),
                            pago.getEstadoPago().name(),
                            pago.getInscripcion() != null ? inscripcionMapper.toDTO(pago.getInscripcion()) : null,
                            pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                            pago.getObservaciones(),
                            detallesFiltrados,
                            pago.getPagoMedios().stream().map(pagoMedioMapper::toDTO).toList(),
                            pago.getTipoPago().toString()
                    );
                })
                .toList();

        // Obtener y procesar la matrícula pendiente (si corresponde).
        MatriculaResponse matriculaDTO = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matriculaDTO != null && matriculaDTO.pagada()) {
            matriculaDTO = null;
        }

        // Obtener las mensualidades pendientes.
        List<MensualidadResponse> mensualidadesPendientesDTO = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        double totalPagos = pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
        double totalMensualidades = mensualidadesPendientesDTO.stream()
                .mapToDouble(m -> m.totalPagar() != null ? m.totalPagar() : 0)
                .sum();
        double totalDeuda = totalPagos + totalMensualidades;
        Alumno alumno = pagosPendientes.isEmpty() ? alumnoRepositorio.findById(alumnoId).orElse(null)
                : pagosPendientes.get(0).getAlumno();
        String alumnoNombre = (alumno != null) ? alumno.getNombre() + " " + alumno.getApellido() : "Desconocido";
        log.info("[listarDeudasPendientesPorAlumno] Total deuda para el alumno id={} es {}", alumnoId, totalDeuda);

        return new DeudasPendientesResponse(
                alumno != null ? alumno.getId() : null,
                alumnoNombre,
                pagosPendientesDTO,
                matriculaDTO,
                mensualidadesPendientesDTO,
                totalDeuda
        );
    }

    private void actualizarDatosPrincipalesPago(Pago pago, PagoRegistroRequest request) {
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setEstadoPago(request.activo() ? EstadoPago.ACTIVO : EstadoPago.ANULADO);
    }

    private void actualizarMetodoPago(Pago pago, Long metodoPagoId) {
        if (metodoPagoId != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
    }

    private double recalcularSaldoRestante(Pago pago) {
        return pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0.0)
                .sum();
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
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId)
                .stream()
                .filter(p -> p.getEstadoPago() != EstadoPago.ANULADO)
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                // Solo pagos con deuda y que no estén anulados
                .filter(p -> p.getSaldoRestante() > 0 && p.getEstadoPago() != EstadoPago.ANULADO)
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
            nuevoPago.setInscripcion(inscripcion);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            verificarSaldoRestante(nuevoPago);
            pagoRepositorio.save(nuevoPago);
        }
    }

    /**
     * Verifica que el saldo restante no sea negativo. Si es negativo, lo ajusta a 0.
     */
    private void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.",
                    pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}",
                    pago.getId(), pago.getSaldoRestante());
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        return pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
    }

    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId);
        return pago != null ? pagoMapper.toDTO(pago) : null;
    }

}
