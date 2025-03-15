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
        log.info("[registrarPago] Iniciando registro de pago para inscripción: {}", request.inscripcion());

        // 1. Se determina el pago final según el flujo (histórico o nuevo).
        Pago pagoFinal = paymentProcessor.crearPagoSegunInscripcion(request);
        log.debug("[registrarPago] Pago generado: id={}, monto={}, saldoRestante={}",
                pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());

        // 2. Marcar como cobrados los detalles cuyo importe pendiente sea 0.
        marcarDetallesCobradosSiImporteEsCero(pagoFinal);

        // 3. Registrar medios de pago, si es que existen.
        registrarMediosDePago(pagoFinal, request.pagoMedios());

        // 4. Actualizar importes del pago (descuentos, recargos, etc.) y recalcular el saldo.
        paymentProcessor.actualizarImportesPago(pagoFinal);
        verificarSaldoRestante(pagoFinal);
        pagoRepositorio.save(pagoFinal);
        log.info("[registrarPago] Pago guardado con id={}, saldoRestante={}", pagoFinal.getId(), pagoFinal.getSaldoRestante());

        // 5. Si el pago está completamente saldado y tiene alumno, se actualizan sus deudas.
        if (pagoFinal.getSaldoRestante() == 0 && pagoFinal.getAlumno() != null) {
            log.info("[registrarPago] Saldo en 0. Actualizando deudas del alumno id={}", pagoFinal.getAlumno().getId());
            actualizarEstadoDeudas(pagoFinal.getAlumno().getId(), pagoFinal.getFecha());
        }

        PagoResponse response = pagoMapper.toDTO(pagoFinal);
        log.debug("[registrarPago] Respuesta generada: {}", response);
        return response;
    }

    private static void verificarSaldoRestante(Pago pagoFinal) {
        if (pagoFinal.getSaldoRestante() < 0) {
            log.error("Error: Saldo restante negativo detectado en pago ID={}. Ajustando a 0.", pagoFinal.getId());
            pagoFinal.setSaldoRestante(0.0);
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
                .collect(Collectors.toList());

        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        log.debug("[actualizarDetallesPago] Detalles actualizados. Recalculando importes para cada detalle.");

        // Recalcular el importe de cada detalle (se preserva el aCobrar acumulado)
        pago.getDetallePagos().forEach(detalle -> {
            log.debug("[actualizarDetallesPago] Recalculando importe para detalle id={}", detalle.getId());
            detallePagoServicio.calcularImporte(detalle);
        });
    }

    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> existentes, Pago pago) {
        if (dto.id() != null && existentes.containsKey(dto.id())) {
            DetallePago detalle = existentes.get(dto.id());
            log.info("[obtenerODefinirDetallePago] Actualizando detalle existente id={}", detalle.getId());
            detalle.setConcepto(dto.concepto());
            detalle.setValorBase(dto.valorBase());
            // No sobrescribir aCobrar para preservar el acumulado
            // Actualizar otros campos:
            detalle.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
            detalle.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            return detalle;
        } else {
            log.info("[obtenerODefinirDetallePago] Creando nuevo detalle para concepto '{}'", dto.concepto());
            DetallePago nuevo = new DetallePago();
            nuevo.setPago(pago);
            nuevo.setConcepto(dto.concepto());
            nuevo.setValorBase(dto.valorBase());
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : dto.valorBase());
            nuevo.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
            nuevo.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            return nuevo;
        }
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
        log.debug("[registrarPagoParcial] Montos por detalle: {}", montosPorDetalle);

        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        log.debug("[registrarPagoParcial] Pago obtenido: id={}, monto={}, saldoRestante={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante());

        MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        log.debug("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}",
                metodo.getId(), metodo.getDescripcion());

        // Se crea el medio de pago
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setMetodo(metodo);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);
        log.debug("[registrarPagoParcial] PagoMedio creado y asignado al pago id={}", pago.getId());

        // Actualización de cada detalle según el abono asignado
        for (DetallePago detalle : pago.getDetallePagos()) {
            if (montosPorDetalle.containsKey(detalle.getId())) {
                double abono = montosPorDetalle.get(detalle.getId());
                log.debug("[registrarPagoParcial] Procesando detalle id={}. Abono recibido: {}", detalle.getId(), abono);
                if (abono < 0) {
                    throw new IllegalArgumentException("No se permite abonar un monto negativo para el detalle id=" + detalle.getId());
                }
                double pendienteActual = detalle.getImportePendiente();
                double nuevoPendiente = pendienteActual - abono;
                if (nuevoPendiente < 0) {
                    nuevoPendiente = 0;
                }
                log.debug("[registrarPagoParcial] Detalle id={} | Pendiente anterior: {} | Nuevo pendiente: {}",
                        detalle.getId(), pendienteActual, nuevoPendiente);
                detalle.setImportePendiente(nuevoPendiente);
                // Se marca como cobrado si el pendiente llega a 0
                if (nuevoPendiente == 0) {
                    detalle.setCobrado(true);
                    log.debug("[registrarPagoParcial] Detalle id={} marcado como cobrado", detalle.getId());
                }
            } else {
                log.debug("[registrarPagoParcial] Detalle id={} sin abono; pendiente se mantiene: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        actualizarImportesPagoParcial(pago);
        log.debug("[registrarPagoParcial] Luego de actualizar importes, saldoRestante={}", pago.getSaldoRestante());

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago actualizado guardado. Nuevo saldoRestante={}", pago.getSaldoRestante());

        PagoResponse response = pagoMapper.toDTO(pago);
        log.debug("[registrarPagoParcial] Respuesta generada: {}", response);
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

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        // Se filtran solo los pagos que NO estén anulados (EstadoPago distinto de ANULADO)
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    double pendiente = detalle.getValorBase() - (detalle.getAFavor() != null ? detalle.getAFavor() : 0.0);
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
        return new CobranzaDTO(alumno.getId(), alumno.getNombre() + " " + alumno.getApellido(), totalPendiente, detalles);
    }

    @Transactional
    public PagoResponse obtenerUltimoPagoPendientePorAlumno(Long alumnoId) {
        Pago pago = pagoRepositorio
                .findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro pago pendiente para el alumno con ID: " + alumnoId));

        List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                .filter(detalle -> !detalle.getCobrado())
                .map(detallePagoMapper::toDTO)
                .toList();

        List<PagoMedioResponse> mediosPago = pago.getPagoMedios().stream()
                .map(pagoMedioMapper::toDTO)
                .toList();

        return new PagoResponse(
                pago.getId(),
                pago.getFecha(),
                pago.getFechaVencimiento(),
                pago.getMonto(),
                pago.getMetodoPago().getDescripcion(),
                pago.getRecargoAplicado(),
                pago.getBonificacionAplicada(),
                pago.getSaldoRestante(),
                pago.getSaldoAFavor(),
                pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO), // valor booleano para 'activo'
                pago.getEstadoPago().name(), // estado en formato String
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
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        List<PagoResponse> pagosPendientesDTO = pagosPendientes.stream()
                .map(pago -> {
                    List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                            .filter(detalle -> !detalle.getCobrado())
                            .map(detallePagoMapper::toDTO)
                            .toList();
                    return new PagoResponse(
                            pago.getId(),
                            pago.getFecha(),
                            pago.getFechaVencimiento(),
                            pago.getMonto(),
                            pago.getMetodoPago().getDescripcion(),
                            pago.getRecargoAplicado(),
                            pago.getBonificacionAplicada(),
                            pago.getSaldoRestante(),
                            pago.getSaldoAFavor(),
                            pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO), // Boolean: true si el estado es ACTIVO
                            pago.getEstadoPago().name(), // Estado en formato String (ACTIVO, HISTORICO, ANULADO)
                            pago.getInscripcion() != null ? inscripcionMapper.toDTO(pago.getInscripcion()) : null,
                            pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                            pago.getObservaciones(),
                            detallesFiltrados,
                            pago.getPagoMedios().stream().map(pagoMedioMapper::toDTO).toList(),
                            pago.getTipoPago().toString()
                    );
                })
                .toList();

        MatriculaResponse matriculaDTO = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matriculaDTO != null && matriculaDTO.pagada()) {
            matriculaDTO = null;
        }

        List<MensualidadResponse> mensualidadesPendientesDTO = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        double totalPagos = pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
        double totalMensualidades = mensualidadesPendientesDTO.stream()
                .mapToDouble(m -> m.totalPagar() != null ? m.totalPagar() : 0)
                .sum();
        double totalDeuda = totalPagos + totalMensualidades;
        Alumno alumno = pagosPendientes.isEmpty() ? alumnoRepositorio.findById(alumnoId).orElse(null)
                : pagosPendientes.get(0).getAlumno();
        String alumnoNombre = (alumno != null) ? alumno.getNombre() + " " + alumno.getApellido() : "Desconocido";

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

    public double calcularDeudaAlumno(Long alumnoId) {
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndEstadoPagoOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        return pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
    }

    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        return pagoMapper.toDTO(paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId));
    }
}
