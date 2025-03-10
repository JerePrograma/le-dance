package ledance.servicios.pago;

import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoModificacionRequest;
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

    // Servicios para delegar la lógica de cálculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        MatriculaServicio matriculaServicio,
                        DetallePagoMapper detallePagoMapper,
                        MensualidadServicio mensualidadServicio, RecargoRepositorio recargoRepositorio, BonificacionRepositorio bonificacionRepositorio, DetallePagoRepositorio detallePagoRepositorio,
                        DetallePagoServicio detallePagoServicio, PaymentProcessor paymentProcessor, DetallePagoMapper detallePagoMapper1, PagoMedioMapper pagoMedioMapper) {
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
    }


    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("Registrando pago para inscripción ID: {}", request.inscripcionId());

        Pago pagoFinal = crearPagoSegunInscripcion(request);
        marcarDetallesCobradosSiImporteEsCero(pagoFinal);

        // Agregar los medios de pago
        registrarMediosDePago(pagoFinal, request.pagoMedios());

        actualizarImportesPago(pagoFinal);
        verificarSaldoRestante(pagoFinal);
        pagoRepositorio.save(pagoFinal);

        if (pagoFinal.getSaldoRestante() == 0 && pagoFinal.getAlumno() != null) {
            actualizarEstadoDeudas(pagoFinal.getAlumno().getId(), pagoFinal.getFecha());
        }

        return pagoMapper.toDTO(pagoFinal);
    }

    private static void verificarSaldoRestante(Pago pagoFinal) {
        if (pagoFinal.getSaldoRestante() < 0) {
            log.error("Error: Saldo restante negativo detectado en pago ID={}. Ajustando a 0.", pagoFinal.getId());
            pagoFinal.setSaldoRestante(0.0);
        }
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoModificacionRequest request) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        actualizarDetallesPago(pago, request.detallePagos());
        log.info("Antes de actualizar - Pago ID={} | SaldoRestante={}", pago.getId(), pago.getSaldoRestante());
        actualizarImportesPago(pago);
        log.info("Después de actualizar - Pago ID={} | SaldoRestante={}", pago.getId(), pago.getSaldoRestante());

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        if (pago.getSaldoRestante() == 0) {
            actualizarEstadoDeudas(pago.getAlumno().getId(), pago.getFecha());
        }

        return pagoMapper.toDTO(pago);
    }

    private void marcarDetallesCobrados(Pago pago) {
        pago.getDetallePagos().forEach(det -> det.setCobrado(det.getImporte() != null && det.getImporte() == 0));
    }

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        Map<Long, DetallePago> existentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));

        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> obtenerODefinirDetallePago(dto, existentes, pago))
                .toList();

        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);
    }

    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> existentes, Pago pago) {
        DetallePago detalle = Optional.ofNullable(dto.id())
                .map(existentes::get)
                .orElseGet(() -> {
                    DetallePago nuevo = new DetallePago();
                    nuevo.setPago(pago);
                    return nuevo;
                });

        detalle.setConcepto(dto.concepto());
        detalle.setValorBase(dto.valorBase());
        detalle.setaCobrar(dto.aCobrar());
        detalle.setImporte(dto.importe());
        detalle.setCobrado(Boolean.TRUE.equals(dto.cobrado()));

        detalle.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                .flatMap(bonificacionRepositorio::findById).orElse(null));
        detalle.setRecargo(dto.recargoId() != null
                ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                : null);

        return detalle;
    }

    private void actualizarImportesPago(Pago pago) {
        pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);

        double total = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImporte)
                .sum();

        pago.setSaldoRestante(total);
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("SaldoRestante actualizado correctamente: {}", total);
    }

    private Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        if (request.inscripcionId() == null || request.inscripcionId() == -1) {
            return paymentProcessor.processGeneralPayment(request);
        }

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Alumno alumno = inscripcion.getAlumno();
        Pago ultimoPendiente = obtenerUltimoPagoPendiente(alumno.getId());

        return (ultimoPendiente != null)
                ? paymentProcessor.processPaymentWithPrevious(request, inscripcion, alumno, ultimoPendiente)
                : paymentProcessor.processFirstPayment(request, inscripcion, alumno);
    }

    private void marcarDetallesCobradosSiImporteEsCero(Pago pago) {
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImporte() != null && detalle.getImporte() == 0.0) {
                detalle.setCobrado(true);
                log.info("Marcando DetallePago ID: {} como COBRADO (importe=0)", detalle.getId());
            }
        });
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    private Pago obtenerUltimoPagoPendiente(Long alumnoId) {
        return pagoRepositorio.findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .filter(p -> p.getSaldoRestante() > 0)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago pendiente para el alumno con ID: " + alumnoId));
    }

    private void actualizarEstadoDeudas(Long alumnoId, LocalDate fechaPago) {
        log.info("Verificando estado de deudas para alumno ID: {}", alumnoId);

        MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matResp != null && !matResp.pagada()) {
            log.info("Actualizando matrícula ID: {} como pagada.", matResp.id());
            matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                    new MatriculaModificacionRequest(matResp.anio(), true, fechaPago));
        } else {
            log.info("No se encontraron matrículas pendientes para el alumno.");
        }

        List<MensualidadResponse> pendientes = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        pendientes.forEach(mens -> {
            if ("PENDIENTE".equalsIgnoreCase(mens.estado()) || "OMITIDO".equalsIgnoreCase(mens.estado())) {
                log.info("Marcando mensualidad ID: {} como pagada.", mens.id());
                mensualidadServicio.marcarComoPagada(mens.id(), fechaPago);
            }
        });

        log.info("Estado de deudas actualizado correctamente.");
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
                    pagoMedio.setMonto(request.monto());
                    pagoMedio.setMetodo(metodo);
                    pagoMedio.setPago(pago);
                    return pagoMedio;
                })
                .toList();

        pago.getPagoMedios().addAll(medios);
    }

    // Método refactorizado para registrar un pago parcial sin usar "abono"
    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado, Map<Long, Double> montosPorDetalle) {
        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        // Se agrega el medio de pago correspondiente
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);

        // Se recorre cada detalle que se desea abonar (ahora se restará de aCobrar)
        pago.getDetallePagos().forEach(detalle -> {
            if (montosPorDetalle.containsKey(detalle.getId())) {
                double montoAplicar = montosPorDetalle.get(detalle.getId());
                // Se descuenta el monto a aplicar de la cantidad pendiente (aCobrar)
                double nuevoACobrar = detalle.getaCobrar() - montoAplicar;
                // Asegurarse de que no sea negativo
                detalle.setaCobrar(nuevoACobrar < 0 ? 0.0 : nuevoACobrar);
                // Recalcula el importe con base en el nuevo aCobrar
                detallePagoServicio.calcularImporte(detalle);
                if (detalle.getaCobrar() == 0.0) {
                    detalle.setCobrado(true);
                }
            }
        });

        actualizarImportesPago(pago);
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        return pagoMapper.toDTO(pago);
    }

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0)
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
        log.info("Obteniendo último pago activo para alumno id: {}", alumnoId);
        Pago pago = pagoRepositorio.findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .filter(p -> p.getSaldoRestante() > 0)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago pendiente para el alumno con ID: " + alumnoId));
        log.info("Último pago obtenido: id={}, monto={}, saldoRestante={}", pago.getId(), pago.getMonto(), pago.getSaldoRestante());

        List<DetallePagoResponse> detallesFiltrados = pago.getSaldoRestante() == 0
                ? Collections.emptyList()
                : pago.getDetallePagos().stream()
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
                pago.getActivo(),
                pago.getEstado(),
                pago.getInscripcion() != null ? pago.getInscripcion().getId() : null,
                pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                pago.getObservaciones(),
                detallesFiltrados,
                mediosPago,
                pago.getTipoPago().toString()
        );
    }

    @Transactional
    public DeudasPendientesResponse listarDeudasPendientesPorAlumno(Long alumnoId) {
        log.info("Consultando deudas pendientes para alumno id: {}", alumnoId);
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0)
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
                            pago.getActivo(),
                            pago.getEstado(),
                            pago.getInscripcion() != null ? pago.getInscripcion().getId() : null,
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

    private void actualizarDatosPrincipalesPago(Pago pago, PagoModificacionRequest request) {
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());
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
        log.info("Obteniendo pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        // Filtrar solo los `DetallePago` que no estén cobrados
        List<DetallePago> detallesPendientes = pago.getDetallePagos().stream()
                .filter(detalle -> !detalle.getCobrado())
                .collect(Collectors.toList());

        pago.setDetallePagos(detallesPendientes);
        return pagoMapper.toDTO(pago);
    }

    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findAll()
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarPago(Long id) {
        log.info("Marcando pago como inactivo: id={}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setActivo(false);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0) // Solo pagos con deuda
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());

    }

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
            verificarSaldoRestante(nuevoPago);
            pagoRepositorio.save(nuevoPago);
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        // Se listan los pagos activos que tienen saldo pendiente
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0) // Solo pagos con saldo pendiente
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0) // Evita pagos saldados
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
        verificarSaldoRestante(pago);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

}
