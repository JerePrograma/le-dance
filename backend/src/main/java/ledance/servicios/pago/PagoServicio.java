package ledance.servicios.pago;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final MetodoPagoMapper metodoPagoMapper;
    private final MatriculaRepositorio matriculaRepositorio;
    private final MensualidadRepositorio mensualidadRepositorio;

    // Servicios para delegar la logica de cálculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;
    private final InscripcionMapper inscripcionMapper;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final DetallePagoRepositorio detallePagoRepositorio;
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
                        DetallePagoRepositorio detallePagoRepositorio,
                        MetodoPagoMapper metodoPagoMapper, MatriculaRepositorio matriculaRepositorio, MensualidadRepositorio mensualidadRepositorio,
                        DetallePagoServicio detallePagoServicio,
                        PaymentProcessor paymentProcessor,
                        PagoMedioMapper pagoMedioMapper,
                        InscripcionMapper inscripcionMapper,
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
        this.metodoPagoMapper = metodoPagoMapper;
        this.matriculaRepositorio = matriculaRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.pagoMedioMapper = pagoMedioMapper;
        this.inscripcionMapper = inscripcionMapper;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.alumnoMapper = alumnoMapper;
    }

    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        // 1) Obtener (opcional) la inscripción del request para logging
        Long inscripcionId = (request.inscripcion() != null ? request.inscripcion().id() : null);
        log.info("[registrarPago] Iniciando registro de pago para inscripción ID={}", inscripcionId);

        try {
            // 2) Crear / actualizar el pago (ver si corresponde cobranza histórica)
            Pago pagoFinal = paymentProcessor.crearPagoSegunInscripcion(request);
            log.info("[registrarPago] Pago generado: id={}, monto={}, saldoRestante={}",
                    pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());

            // 3) Marcar detalles con importePendiente==0 como cobrados (si aplica en tu lógica)
            marcarDetallesConImportePendienteCero(pagoFinal);

            // 4) Registrar medios de pago (tarjeta, efectivo, etc.) según tu request
            registrarMediosDePago(pagoFinal, request.pagoMedios());

            // 5) Actualizar importes parciales del pago (por ejemplo, si PaymentProcessor no lo hace completo)
            actualizarImportesPagoParcial(pagoFinal);

            log.info("[registrarPago] Tras actualizar importes: montoPagado={}, saldoRestante={}",
                    pagoFinal.getMontoPagado(), pagoFinal.getSaldoRestante());

            // 6) Asegurarse de setear 'montoBasePago' antes de persistir (Opción A)
            //    También se reasigna el alumno, por si cambió algo al mapear.
            pagoFinal.setMontoBasePago(pagoFinal.getMonto());
            // 7) Persistimos el pago ya con el campo 'montoBasePago' lleno
            pagoRepositorio.save(pagoFinal);

            // 8) Actualizar deudas (según tu lógica: si se saldó, marcar deuda como cerrada, etc.)
            actualizarDeudasSiCorrespondiente(pagoFinal);

            // 9) Convertimos la entidad a DTO y retornamos
            PagoResponse response = pagoMapper.toDTO(pagoFinal);
            log.info("[registrarPago] Pago registrado con éxito, respuesta: {}", response);

            return response;
        } catch (Exception e) {
            log.error("[registrarPago] Error durante el registro de pago: ", e);
            throw e;
        }
    }

    private Pago marcarDetallesConImportePendienteCero(Pago pago) {
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                detalle.setCobrado(true);
            }
        });
        return verificarSaldoRestante(pago);
    }

    private void actualizarDeudasSiCorrespondiente(Pago pago) {
        List<TipoDetallePago> tiposDeuda = Arrays.asList(TipoDetallePago.MATRICULA, TipoDetallePago.MENSUALIDAD);
        boolean tieneDetalleDeuda = pago.getDetallePagos().stream().anyMatch(det -> tiposDeuda.contains(det.getTipo()));
        if (pago.getSaldoRestante() == 0 && pago.getAlumno() != null && pago.getInscripcion() != null && tieneDetalleDeuda) {
            log.info("[actualizarDeudasSiCorrespondiente] Actualizando deudas para alumno id={} y inscripción id={}",
                    pago.getAlumno().getId(), pago.getInscripcion().getId());
            actualizarEstadoDeudas(pago.getAlumno().getId(), pago.getFecha());
        } else {
            log.info("[actualizarDeudasSiCorrespondiente] No se cumple la condición para actualizar deudas. " +
                            "Detalles: saldoRestante={}, alumnoId={}, inscripcionId={}, tieneDetalleDeuda={}",
                    pago.getSaldoRestante(),
                    (pago.getAlumno() != null ? pago.getAlumno().getId() : "null"),
                    (pago.getInscripcion() != null ? pago.getInscripcion().getId() : "null"),
                    tieneDetalleDeuda);
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
            actualizarImportesPagoParcial(pago);
        }

        verificarSaldoRestante(pago);
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
                .collect(Collectors.toList());
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
            detalle.setMontoOriginal(dto.montoOriginal());
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
            nuevo.setMontoOriginal(dto.montoOriginal());
            // Asigna el nuevo campo unificado para la descripción.
            nuevo.setDescripcionConcepto(dto.descripcionConcepto());
            // Si aCobrar está definido y es mayor a 0, se utiliza; de lo contrario se usa montoOriginal.
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : dto.montoOriginal());
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
                    // Se calcula el pendiente usando montoOriginal y aFavor.
                    double aFavor = detalle.getAFavor() != null ? detalle.getAFavor() : 0.0;
                    double pendiente = detalle.getMontoOriginal() - aFavor;
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

    public PagoResponse obtenerUltimoPagoPendiente(Long alumnoId) {
        // Se asume que el repositorio ha sido actualizado para filtrar por EstadoPago.ACTIVO
        Optional<Pago> pagoOpt = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0);
        return pagoOpt.map(pagoMapper::toDTO).orElse(null);
    }

    private void actualizarEstadoDeudas(Long alumnoId, LocalDate fechaPago) {

        MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matResp != null && !matResp.pagada()) {
            matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                    new MatriculaRegistroRequest(alumnoId, matResp.anio(), true, fechaPago));
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
                metodoPagoMapper.toDTO(pago.getMetodoPago()),
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
        log.info("Inicio listarDeudasPendientesPorAlumno para alumnoId: {}", alumnoId);

        // 1. Obtener todos los DetallePago con importePendiente > 0.
        List<DetallePago> detallesPendientes = detallePagoRepositorio
                .findByAlumnoIdAndImportePendienteGreaterThan(alumnoId, 0.0);
        log.info("Detalles de pago pendientes encontrados: {}", detallesPendientes.size());

        // 2. Obtener Matrículas pendientes (no pagadas) del alumno.
        List<Matricula> matriculasPendientes = matriculaRepositorio
                .findByAlumnoIdAndPagadaFalse(alumnoId);
        log.info("Matrículas pendientes encontradas: {}", matriculasPendientes.size());

        // 3. Obtener Mensualidades pendientes (estado PENDIENTE) del alumno.
        List<Mensualidad> mensualidadesPendientes = mensualidadRepositorio
                .findByInscripcionAlumnoIdAndEstado(alumnoId, EstadoMensualidad.PENDIENTE);
        log.info("Mensualidades pendientes encontradas: {}", mensualidadesPendientes.size());

        // 4. Convertir cada lista a DTO unificado.
        List<DetallePagoResponse> detallePagosDTO = new ArrayList<>();

        // 4a. Convertir DetallePago a DTO
        List<DetallePagoResponse> detallesFromPago = detallesPendientes.stream()
                .map(detallePagoMapper::toDTO)
                .collect(Collectors.toList());
        detallePagosDTO.addAll(detallesFromPago);

        // 4b. Convertir Matrículas pendientes a DTO.
        List<DetallePagoResponse> matriculasDTO = matriculasPendientes.stream().map(matricula -> {
            return new DetallePagoResponse(
                    matricula.getId(),                               // id
                    "Matrícula " + matricula.getAnio(),              // descripcionConcepto
                    matricula.getAnio().toString(),                  // cuota (se puede ajustar según lógica)
                    0.0,                                             // montoOriginal (ajustar si corresponde)
                    null,                                            // bonificacionId
                    null,                                            // recargoId
                    0.0,                                             // aFavor
                    0.0,                                             // aCobrar
                    false,                                           // cobrado
                    null,                                            // conceptoId
                    null,                                            // subConceptoId
                    null,                                            // mensualidadId
                    matricula.getId(),                               // matriculaId
                    null,                                            // stockId
                    0.0,                                             // importeInicial (ajustar según la deuda)
                    0.0,                                             // importePendiente (valor pendiente)
                    "MATRICULA",                                     // tipo
                    matricula.getFechaPago() != null ? matricula.getFechaPago() : LocalDate.now() // fechaRegistro
            );
        }).collect(Collectors.toList());
        detallePagosDTO.addAll(matriculasDTO);

        // 4c. Convertir Mensualidades pendientes a DTO.
        // Primero, obtener los IDs de mensualidades que ya se encuentran registradas en DetallePago.
        Set<Long> mensualidadesRegistradas = detallesFromPago.stream()
                .filter(dto -> dto.mensualidadId() != null)
                .map(DetallePagoResponse::mensualidadId)
                .collect(Collectors.toSet());

        List<DetallePagoResponse> mensualidadesDTO = mensualidadesPendientes.stream()
                // Filtrar: solo incluir si no está registrada y si aún tiene importe pendiente.
                .filter(mensualidad -> mensualidad.getImportePendiente() > 0
                        && !mensualidadesRegistradas.contains(mensualidad.getId()))
                .map(mensualidad -> {
                    // Formatear la fecha de cuota para obtener el período (ej. "MARZO 2025")
                    String periodo = mensualidad.getFechaCuota()
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                            .toUpperCase();
                    return new DetallePagoResponse(
                            mensualidad.getId(),                              // id (usamos el id de la mensualidad)
                            mensualidad.getDescripcion() != null
                                    ? mensualidad.getDescripcion()
                                    : "Mensualidad " + periodo,                // descripcionConcepto
                            periodo,                                          // cuota: el período registrado
                            mensualidad.getValorBase(),                       // montoOriginal
                            mensualidad.getBonificacion() != null
                                    ? mensualidad.getBonificacion().getId() : null,  // bonificacionId
                            mensualidad.getRecargo() != null
                                    ? mensualidad.getRecargo().getId() : null,       // recargoId
                            0.0,                                              // aFavor
                            mensualidad.getValorBase(),                       // aCobrar (valor base; frontend siempre podrá ajustarlo)
                            false,                                            // cobrado
                            null,                                             // conceptoId
                            null,                                             // subConceptoId
                            mensualidad.getId(),                              // mensualidadId
                            null,                                             // matriculaId
                            null,                                             // stockId
                            mensualidad.getImporteInicial(),                  // importeInicial
                            mensualidad.getImporteInicial() - mensualidad.getMontoAbonado(), // importePendiente
                            "MENSUALIDAD",                                    // tipo
                            mensualidad.getFechaCuota()                       // fechaRegistro
                    );
                })
                .collect(Collectors.toList());
        detallePagosDTO.addAll(mensualidadesDTO);

        // 5. Calcular el total de deuda (sumar importePendiente de cada elemento).
        double totalDeuda = detallePagosDTO.stream()
                .mapToDouble(item -> item.importePendiente() != null ? item.importePendiente() : 0.0)
                .sum();
        log.info("Total deuda calculada: {}", totalDeuda);

        // 6. Obtener los datos del alumno.
        Alumno alumno = alumnoRepositorio.findById(alumnoId).orElse(null);
        String alumnoNombre = alumno != null ? alumno.getNombre() + " " + alumno.getApellido() : "Desconocido";
        log.info("Datos del alumno: id={}, nombre={}", alumno != null ? alumno.getId() : "null", alumnoNombre);

        // 7. Crear y retornar la respuesta unificada.
        DeudasPendientesResponse respuesta = new DeudasPendientesResponse(
                alumno != null ? alumno.getId() : null,
                alumnoNombre,
                detallePagosDTO,
                totalDeuda
        );
        log.info("Respuesta DeudasPendientesResponse generada: {}", respuesta);
        log.info("Fin listarDeudasPendientesPorAlumno");

        return respuesta;
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
            nuevoPago.setInscripcion(inscripcion);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            verificarSaldoRestante(nuevoPago);
            pagoRepositorio.save(nuevoPago);
        }
    }

    /**
     * Verifica que el saldo restante no sea negativo. Si es negativo, lo ajusta a 0.
     */
    private Pago verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.",
                    pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es correcto: {}",
                    pago.getId(), pago.getSaldoRestante());
        }
        return pago;
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
