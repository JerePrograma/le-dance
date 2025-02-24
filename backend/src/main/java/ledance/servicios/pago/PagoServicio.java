package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.*;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.inscripcion.InscripcionServicio;
import ledance.servicios.matricula.MatriculaServicio;
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
    private final PaymentCalculationService calculationService;
    private final MatriculaServicio matriculaServicio;
    private final InscripcionServicio inscripcionServicio;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio, PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        PaymentCalculationService calculationService,
                        MatriculaServicio matriculaServicio,
                        InscripcionServicio inscripcionServicio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.calculationService = calculationService;
        this.matriculaServicio = matriculaServicio;
        this.inscripcionServicio = inscripcionServicio;
    }

    @Override
    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("Registrando pago para inscripcionId: {}", request.inscripcionId());

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

        MetodoPago metodoPago = null;
        if (request.metodoPagoId() != null) {
            metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        }

        double costoBase = calculationService.calcularCostoBase(inscripcion);
        double costoFinal = calculationService.calcularCostoFinal(request, inscripcion, costoBase);
        double sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcion.getId())
                .stream().mapToDouble(Pago::getMonto).sum();

        if (metodoPago != null && "DEBITO".equalsIgnoreCase(metodoPago.getDescripcion())) {
            costoFinal += 5000;
        }

        double saldoRestante = costoFinal - sumPagosPrevios;

        Pago pago = pagoMapper.toEntity(request);
        pago.setMonto(costoFinal);
        pago.setSaldoRestante(saldoRestante);
        pago.setSaldoAFavor(0.0);
        pago.setInscripcion(inscripcion);
        pago.setMetodoPago(metodoPago);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }

        Pago guardado = pagoRepositorio.save(pago);

        if (saldoRestante < 0) {
            Alumno alumno = inscripcion.getAlumno();
            alumno.setDeudaPendiente(true);
            alumnoRepositorio.save(alumno);
        }

        if (Boolean.TRUE.equals(request.pagoMatricula())) {
            pago.setSaldoAFavor(request.monto());
            Long alumnoId = inscripcion.getAlumno().getId();
            var matriculaResponse = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
            matriculaServicio.actualizarEstadoMatricula(matriculaResponse.id(),
                    new MatriculaModificacionRequest(true, LocalDate.now()));
        }

        return pagoMapper.toDTO(guardado);
    }

    @Override
    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
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
        log.info("Actualizando pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setSaldoRestante(request.saldoRestante());
        pago.setActivo(request.activo());
        if (request.metodoPagoId() != null) {
            MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodoPago);
        }
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }

        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPago(Long id) {
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
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PagoResponse> listarPagosVencidos() {
        return pagoRepositorio.findPagosVencidos(LocalDate.now()).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void generarCuotasParaAlumnosActivos() {
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            double costoBase = calculationService.calcularCostoBase(inscripcion);
            // Aquí se puede ajustar la lógica para bonificaciones y recargos si aplica.
            double costoFinal = costoBase;
            Pago nuevoPago = new Pago();
            nuevoPago.setFecha(LocalDate.now());
            nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
            nuevoPago.setMonto(costoFinal);
            nuevoPago.setSaldoRestante(costoFinal);
            nuevoPago.setActivo(true);
            nuevoPago.setInscripcion(inscripcion);
            pagoRepositorio.save(nuevoPago);
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
                .collect(Collectors.toList());
        return pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
    }

    public String getEstadoPago(Pago pago) {
        return pago.getEstado();
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setRecargoAplicado(false);
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                detalle.setRecargo(0.0);
                detalle.calcularImporte();
            }
        }
        double nuevoMonto = pago.getDetallePagos().stream().mapToDouble(DetallePago::getImporte).sum();
        pago.setMonto(nuevoMonto);
        double sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(pago.getInscripcion().getId())
                .stream().mapToDouble(Pago::getMonto).sum();
        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    // Nuevo método para agrupar ítems pendientes en una cobranza
    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
                .collect(Collectors.toList());

        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;

        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    if (detalle.getACobrar() > 0) {
                        String concepto = detalle.getConcepto();
                        double pendiente = detalle.getACobrar();
                        conceptosPendientes.put(concepto, conceptosPendientes.getOrDefault(concepto, 0.0) + pendiente);
                        totalPendiente += pendiente;
                    }
                }
            }
        }

        List<DetalleCobranzaDTO> detalles = conceptosPendientes.entrySet().stream()
                .map(e -> new DetalleCobranzaDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        CobranzaDTO cobranza = new CobranzaDTO(alumno.getId(), alumno.getNombre() + " " + alumno.getApellido(), totalPendiente, detalles);
        return cobranza;
    }
}
