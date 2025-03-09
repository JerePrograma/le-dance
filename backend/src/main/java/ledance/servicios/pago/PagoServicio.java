package ledance.servicios.pago;

import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.DetallePago;
import ledance.entidades.Pago;
import ledance.entidades.PagoMedio;
import ledance.entidades.Alumno;
import ledance.entidades.Inscripcion;
import ledance.entidades.EstadoInscripcion;
import ledance.repositorios.PagoRepositorio;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.detallepago.DetallePagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PagoServicio implements IPagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final DetallePagoMapper detallePagoMapper;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;

    // Servicios para delegar la lógica de cálculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentProcessor paymentProcessor;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        MatriculaServicio matriculaServicio,
                        DetallePagoMapper detallePagoMapper,
                        MensualidadServicio mensualidadServicio,
                        DetallePagoServicio detallePagoServicio, PaymentProcessor paymentProcessor) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadServicio = mensualidadServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
    }

    @Override
    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("[PagoServicio] Iniciando registro de pago para inscriptionId: {}", request.inscripcionId());
        log.debug("[PagoServicio] Payload recibido: {}", request);
        Pago pagoFinal;
        if (request.inscripcionId() != null && request.inscripcionId() == -1) {
            log.info("[PagoServicio] Se procesa pago GENERAL (sin inscripción).");
            pagoFinal = paymentProcessor.processGeneralPayment(request);
        } else {
            assert request.inscripcionId() != null;
            log.info("[PagoServicio] Se detecta pago con inscripción. Procesando actualización o creación.");
            Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                    .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
            Alumno alumno = inscripcion.getAlumno();
            boolean hayDetalleNuevos = request.detallePagos().stream().anyMatch(det -> det.id() == null);
            List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumno.getId())
                    .stream()
                    .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                    .toList();
            Pago pagoAnterior = pagosPendientes.isEmpty() ? null : pagosPendientes.get(0);
            log.info("[PagoServicio] Pago anterior: {}", pagoAnterior);
            if (hayDetalleNuevos) {
                if (pagoAnterior != null) {
                    log.info("[PagoServicio] Procesando actualización con pago anterior (id {}).", pagoAnterior.getId());
                    pagoFinal = paymentProcessor.processPaymentWithPrevious(request, inscripcion, alumno, pagoAnterior);
                } else {
                    log.info("[PagoServicio] Procesando primer pago para inscripción.");
                    pagoFinal = paymentProcessor.processFirstPayment(request, inscripcion, alumno);
                }
            } else {
                log.info("[PagoServicio] Procesando pago sin nuevos detalles.");
                pagoFinal = paymentProcessor.processPaymentWithoutNewDetails(request, inscripcion);
            }
            log.info("[PagoServicio] Pago final procesado: id={}, monto={}, saldoRestante={}",
                    pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());
            // Actualizar matrícula y mensualidades si el pago queda completamente abonado.
            if (pagoFinal.getSaldoRestante() == 0) {
                log.info("Pago completamente abonado. Procesando matrícula y mensualidades para alumno id {}.", alumno.getId());
                MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
                if (matResp != null && !matResp.pagada()) {
                    matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                            new MatriculaModificacionRequest(matResp.anio(), true, pagoFinal.getFecha()));
                }
                List<MensualidadResponse> pendientes = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumno.getId());
                pendientes.forEach(mens -> {
                    if ("PENDIENTE".equalsIgnoreCase(mens.estado()) || "OMITIDO".equalsIgnoreCase(mens.estado())) {
                        mensualidadServicio.marcarComoPagada(mens.id(), pagoFinal.getFecha());
                    }
                });
            }
            log.info("[PagoServicio] Pago final registrado: id={}, monto={}, saldoRestante={}",
                    pagoFinal.getId(), pagoFinal.getMonto(), pagoFinal.getSaldoRestante());
        }
        return pagoMapper.toDTO(pagoFinal);
    }

    /**
     * Método auxiliar que actualiza los importes de cada detalle utilizando DetallePagoServicio
     * y recalcula el saldo restante en el pago.
     */
    private void actualizarImportesPago(Pago pago) {
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);
        }
        if (pago.getDetallePagos() != null && !pago.getDetallePagos().isEmpty()) {
            double totalImporte = pago.getDetallePagos().stream()
                    .mapToDouble(det -> det.getImporte() != null ? det.getImporte() : 0.0)
                    .sum();
            pago.setSaldoRestante(totalImporte);
        } else {
            pago.setSaldoRestante(pago.getMonto());
        }
    }

    @Override
    public PagoResponse obtenerPagoPorId(Long id) {
        log.info("Obteniendo pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        // Actualizamos los importes de cada detalle antes de devolverlo.
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> {
                double importeCalculado = (detalle.getaCobrar() != null ?
                        detalle.getValorBase() - detalle.getaCobrar() : detalle.getValorBase());
                detalle.setImporte(Math.max(importeCalculado, 0));
            });
        }
        return pagoMapper.toDTO(pago);
    }

    @Override
    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findAll()
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PagoResponse actualizarPago(Long id, PagoModificacionRequest request) {
        log.info("Iniciando actualización del pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());
        if (request.metodoPagoId() != null) {
            pago.setMetodoPago(metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado.")));
        }
        if (request.detallePagos() != null && !request.detallePagos().isEmpty()) {
            Map<Long, DetallePago> detallesExistentes = pago.getDetallePagos().stream()
                    .collect(Collectors.toMap(DetallePago::getId, d -> d));
            List<DetallePago> detallesActualizados = new ArrayList<>();
            for (var detalleDTO : request.detallePagos()) {
                DetallePago detalle;
                if (detalleDTO.id() != null && detallesExistentes.containsKey(detalleDTO.id())) {
                    detalle = detallesExistentes.get(detalleDTO.id());
                    if (detalle.getImporte() != null && detalle.getImporte() == 0) {
                        detallesActualizados.add(detalle);
                        continue;
                    }
                } else {
                    detalle = detallePagoMapper.toEntity(detalleDTO);
                    detalle.setPago(pago);
                }
                detalle.setaCobrar(detalleDTO.aCobrar() != null ? detalleDTO.aCobrar() : 0);
                // Se recalcula el importe usando la lógica del servicio de detalle.
                detallePagoServicio.calcularImporte(detalle);
                detallesActualizados.add(detalle);
            }
            pago.getDetallePagos().clear();
            pago.getDetallePagos().addAll(detallesActualizados);
        }
        double totalCobrado = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0)
                .sum();
        double saldoRestante = pago.getMonto() - totalCobrado;
        pago.setSaldoRestante(saldoRestante < 0 ? 0 : saldoRestante);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPago(Long id) {
        log.info("Marcando pago como inactivo: id={}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setActivo(false);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        pagoRepositorio.save(pago);
    }

    @Override
    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PagoResponse> listarPagosVencidos() {
        LocalDate hoy = LocalDate.now();
        return pagoRepositorio.findPagosVencidos(hoy)
                .stream()
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
            nuevoPago.setActivo(true);
            nuevoPago.setInscripcion(inscripcion);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            pagoRepositorio.save(nuevoPago);
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
                .toList();
        return pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
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
            }
        }
        assert pago.getDetallePagos() != null;
        double nuevoMonto = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImporte)
                .sum();
        pago.setMonto(nuevoMonto);
        double sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(
                        pago.getInscripcion().getId()).stream()
                .mapToDouble(Pago::getMonto)
                .sum();
        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado, Map<Long, Double> abonosPorDetalle) {
        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);
        pago.getDetallePagos().forEach(detalle -> {
            if (abonosPorDetalle.containsKey(detalle.getId())) {
                Double abono = abonosPorDetalle.get(detalle.getId());
                detalle.setAbono(abono);
                detallePagoServicio.calcularImporte(detalle);
            }
        });
        // Se recalcula el saldo restante del pago.
        actualizarImportesPago(pago);
        Pago guardado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(guardado);
    }

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
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
    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        // Se busca el último pago activo del alumno, ordenado por fecha descendente.
        Pago pago = pagoRepositorio.findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago para el alumno con ID: " + alumnoId));
        log.info("Último pago activo obtenido para alumno id {}: id={}, monto={}", alumnoId, pago.getId(), pago.getMonto());
        return pagoMapper.toDTO(pago);
    }

    @Transactional
    public DeudasPendientesResponse listarDeudasPendientesPorAlumno(Long alumnoId) {
        log.info("Consultando deudas pendientes para alumno id: {}", alumnoId);

        // Se listan los pagos activos que tienen saldo pendiente
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .toList();

        List<PagoResponse> pagosPendientesDTO = pagosPendientes.stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());

        // Se consulta la matrícula pendiente, si es que existe y no se encuentra pagada
        MatriculaResponse matriculaDTO = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matriculaDTO != null && matriculaDTO.pagada()) {
            matriculaDTO = null;
        }

        // Se obtienen las mensualidades pendientes para el alumno
        List<MensualidadResponse> mensualidadesPendientesDTO = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);

        // Se calcula el total pendiente en pagos y mensualidades
        double totalPagos = pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
        double totalMensualidades = mensualidadesPendientesDTO.stream()
                .mapToDouble(m -> m.totalPagar() != null ? m.totalPagar() : 0)
                .sum();
        double totalDeuda = totalPagos + totalMensualidades;

        // Para obtener el nombre del alumno se utiliza el alumno asociado al último pago pendiente, o se consulta directamente
        Alumno alumno = pagosPendientes.isEmpty() ? alumnoRepositorio.findById(alumnoId).orElse(null)
                : pagosPendientes.get(0).getAlumno();
        String alumnoNombre = (alumno != null) ? alumno.getNombre() + " " + alumno.getApellido() : "Desconocido";

        log.info("Deuda total para alumno id {}: {}", alumnoId, totalDeuda);

        return new DeudasPendientesResponse(
                alumno != null ? alumno.getId() : null,
                alumnoNombre,
                pagosPendientesDTO,
                matriculaDTO,
                mensualidadesPendientesDTO,
                totalDeuda
        );
    }

}
