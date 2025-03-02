package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Join;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.entidades.*;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.RecargoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class MensualidadServicio implements IMensualidadService {

    private static final Logger log = LoggerFactory.getLogger(MensualidadServicio.class);

    private final MensualidadRepositorio mensualidadRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MensualidadMapper mensualidadMapper;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    public MensualidadServicio(MensualidadRepositorio mensualidadRepositorio,
                               InscripcionRepositorio inscripcionRepositorio,
                               MensualidadMapper mensualidadMapper,
                               RecargoRepositorio recargoRepositorio,
                               BonificacionRepositorio bonificacionRepositorio) {
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.mensualidadMapper = mensualidadMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
    }

    @Override
    public MensualidadResponse crearMensualidad(MensualidadRegistroRequest request) {
        log.info("Creando mensualidad para inscripción id: {}", request.inscripcionId());

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        Recargo recargo = determinarRecargo(request);

        Bonificacion bonificacion = (request.bonificacionId() != null) ?
                bonificacionRepositorio.findById(request.bonificacionId())
                        .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada"))
                : null;

        Mensualidad mensualidad = mensualidadMapper.toEntity(request);
        mensualidad.setInscripcion(inscripcion);
        mensualidad.setRecargo(recargo);
        mensualidad.setBonificacion(bonificacion);
        mensualidad.calcularTotal();
        log.info("Total a pagar calculado: {}", mensualidad.getTotalPagar());

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad creada con id: {}", mensualidad.getId());
        return mensualidadMapper.toDTO(mensualidad);
    }

    private Recargo determinarRecargo(MensualidadRegistroRequest request) {
        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            log.info("Recargo asignado por id: {}", recargo.getId());
            return recargo;
        } else {
            int cuotaDay = request.fechaCuota().getDayOfMonth();
            Recargo recargo = recargoRepositorio.findAll().stream()
                    .filter(r -> cuotaDay > r.getDiaDelMesAplicacion())
                    .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                    .orElse(null);
            if (recargo != null) {
                log.info("Recargo determinado automáticamente: id={}, diaAplicacion={}", recargo.getId(), recargo.getDiaDelMesAplicacion());
            } else {
                log.info("No se determinó recargo automáticamente para el día: {}", cuotaDay);
            }
            return recargo;
        }
    }

    @Override
    public MensualidadResponse actualizarMensualidad(Long id, MensualidadModificacionRequest request) {
        log.info("Actualizando mensualidad id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));

        mensualidad.setFechaCuota(request.fechaCuota());
        mensualidad.setValorBase(request.valorBase());
        mensualidad.setEstado(Enum.valueOf(EstadoMensualidad.class, request.estado()));

        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            mensualidad.setRecargo(recargo);
        } else {
            mensualidad.setRecargo(null);
        }

        if (request.bonificacionId() != null) {
            Bonificacion bonificacion = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada"));
            mensualidad.setBonificacion(bonificacion);
        } else {
            mensualidad.setBonificacion(null);
        }

        mensualidad.calcularTotal();
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad actualizada: id={}, totalPagar={}", mensualidad.getId(), mensualidad.getTotalPagar());
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
    public MensualidadResponse obtenerMensualidad(Long id) {
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
    public List<MensualidadResponse> listarMensualidades() {
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findAll()
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de mensualidades listadas: {}", respuestas.size());
        return respuestas;
    }

    @Override
    public List<MensualidadResponse> listarPorInscripcion(Long inscripcionId) {
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findByInscripcionId(inscripcionId)
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Mensualidades encontradas para inscripción id {}: {}", inscripcionId, respuestas.size());
        return respuestas;
    }

    @Override
    public void eliminarMensualidad(Long id) {
        if (!mensualidadRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Mensualidad no encontrada");
        }
        mensualidadRepositorio.deleteById(id);
        log.info("Mensualidad eliminada: id={}", id);
    }

    /**
     * Busca mensualidades según filtros y devuelve un Page de ReporteMensualidadDTO.
     * Filtros:
     * - fechaInicio y fechaFin: rango de fechas aplicable a Mensualidad.fechaCuota.
     * - disciplinaId: opcional, filtra por el ID de la Disciplina (a través de la Inscripción).
     * - profesorId: opcional, filtra por el ID del Profesor (desde Disciplina en la Inscripción).
     */
    // Suponiendo que modificamos el método para recibir también "disciplinaNombre" y "profesorNombre"
    public List<ReporteMensualidadDTO> buscarMensualidades(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String disciplinaNombre,
            String profesorNombre
    ) {
        log.info("Buscando mensualidades con fecha entre {} y {}, disciplinaNombre={}, profesorNombre={}",
                fechaInicio, fechaFin, disciplinaNombre, profesorNombre);

        Specification<Mensualidad> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("fechaCuota"), fechaInicio, fechaFin)
        );

        if (disciplinaNombre != null && !disciplinaNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                // Join de Mensualidad -> Inscripcion -> Disciplina
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                return cb.like(cb.lower(disciplina.get("nombre")), "%" + disciplinaNombre.toLowerCase() + "%");
            });
        }

        if (profesorNombre != null && !profesorNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                // Join de Mensualidad -> Inscripcion -> Disciplina -> Profesor
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                Join<Disciplina, Profesor> profesor = disciplina.join("profesor");
                return cb.like(cb.lower(profesor.get("nombre")), "%" + profesorNombre.toLowerCase() + "%");
            });
        }

        List<Mensualidad> mensualidades = mensualidadRepositorio.findAll(spec);
        log.info("Total de mensualidades encontradas: {}", mensualidades.size());

        // Utilizamos el método mapearReporte para transformar cada entidad en un ReporteMensualidadDTO.
        return mensualidades.stream()
                .map(this::mapearReporte)
                .collect(Collectors.toList());
    }

    public ReporteMensualidadDTO mapearReporte(Mensualidad mensualidad) {
        String alumnoNombre = mensualidad.getInscripcion().getAlumno().getNombre() + " " +
                mensualidad.getInscripcion().getAlumno().getApellido();

        // Determinar el tipo de cuota usando el método auxiliar:
        String cuota = determinarTipoCuota(mensualidad);

        Double importe = mensualidad.getValorBase();

        double bonificacion = 0.0;
        if (mensualidad.getBonificacion() != null) {
            double valorFijo = mensualidad.getBonificacion().getValorFijo() != null
                    ? mensualidad.getBonificacion().getValorFijo() : 0.0;
            double porcentaje = mensualidad.getBonificacion().getPorcentajeDescuento() != null
                    ? mensualidad.getBonificacion().getPorcentajeDescuento() / 100.0 * mensualidad.getValorBase()
                    : 0.0;
            bonificacion = valorFijo + porcentaje;
        }

        double recargo = 0.0;
        if (mensualidad.getRecargo() != null) {
            double recargoFijo = mensualidad.getRecargo().getValorFijo() != null
                    ? mensualidad.getRecargo().getValorFijo() : 0.0;
            double recargoPorcentaje = mensualidad.getRecargo().getPorcentaje() != null
                    ? mensualidad.getRecargo().getPorcentaje() / 100.0 * mensualidad.getValorBase()
                    : 0.0;
            recargo = recargoFijo + recargoPorcentaje;
        }

        // Se puede utilizar el método calcularTotal() o realizar la fórmula de forma explícita:
        Double total = importe - bonificacion + recargo;

        String estado = mensualidad.getEstado() == EstadoMensualidad.PAGADO ? "Abonado" : "Pendiente";

        String disciplina = mensualidad.getInscripcion().getDisciplina().getNombre();

        return new ReporteMensualidadDTO(
                mensualidad.getId(),
                alumnoNombre,
                cuota,
                importe,
                bonificacion,
                total,
                recargo,
                estado,
                disciplina
        );
    }

    public String determinarTipoCuota(Mensualidad mensualidad) {
        Double valorBase = mensualidad.getValorBase();
        Disciplina disciplina = mensualidad.getInscripcion().getDisciplina();

        if (valorBase.equals(disciplina.getValorCuota())) {
            return "CUOTA";
        } else if (valorBase.equals(disciplina.getClaseSuelta())) {
            return "CLASE SUELTA";
        } else if (valorBase.equals(disciplina.getClasePrueba())) {
            return "CLASE DE PRUEBA";
        }
        // En caso de que no coincida con ninguno, se puede devolver un valor por defecto:
        return "CUOTA";
    }

}
