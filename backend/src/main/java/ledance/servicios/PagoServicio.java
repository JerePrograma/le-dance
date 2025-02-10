package ledance.servicios;

import ledance.dto.mappers.PagoMapper;
import ledance.dto.request.PagoModificacionRequest;
import ledance.dto.request.PagoRegistroRequest;
import ledance.dto.response.PagoResponse;
import ledance.entidades.Inscripcion;
import ledance.entidades.MetodoPago;
import ledance.entidades.Pago;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServicio implements IPagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    private final PagoRepositorio pagoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;

    public PagoServicio(PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper) {
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
    }

    /**
     * ✅ Registrar un nuevo pago
     */
    @Override
    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("Registrando pago para inscripcionId: {}", request.inscripcionId());

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));

        MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado."));

        Pago pago = pagoMapper.toEntity(request);
        pago.setInscripcion(inscripcion);
        pago.setMetodoPago(metodoPago);

        Pago guardado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(guardado);
    }

    /**
     * ✅ Obtener un pago por ID
     */
    @Override
    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        return pagoMapper.toDTO(pago);
    }

    /**
     * ✅ Listar solo pagos activos
     */
    @Override
    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findByActivoTrue().stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Actualizar un pago
     */
    @Override
    @Transactional
    public PagoResponse actualizarPago(Long id, PagoModificacionRequest request) {
        log.info("Actualizando pago con id: {}", id);

        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        pago.setFecha(request.fecha());
        pago.setMonto(request.monto());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setSaldoRestante(request.saldoRestante());

        if (request.metodoPagoId() != null) {
            MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado."));
            pago.setMetodoPago(metodoPago);
        }

        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    /**
     * ✅ Baja logica de un pago
     */
    @Override
    @Transactional
    public void eliminarPago(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setActivo(false);
        pagoRepositorio.save(pago);
    }

    /**
     * ✅ Listar pagos por inscripcion, ordenados por fecha descendente.
     */
    @Override
    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Listar pagos por alumno, ordenados por fecha descendente.
     */
    @Override
    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Listar pagos vencidos
     */
    @Override
    public List<PagoResponse> listarPagosVencidos() {
        return pagoRepositorio.findPagosVencidos(LocalDate.now()).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }
}
