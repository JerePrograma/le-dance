package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.dto.asistencia.AsistenciaDiariaMapper;
import ledance.entidades.*;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaDiariaServicio {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaDiariaServicio.class);

    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final AsistenciaDiariaMapper asistenciaDiariaMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final DisciplinaServicio disciplinaServicio;

    public AsistenciaDiariaServicio(
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            AsistenciaDiariaMapper asistenciaDiariaMapper,
            InscripcionRepositorio inscripcionRepositorio,
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            DisciplinaServicio disciplinaServicio) {
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaDiariaMapper = asistenciaDiariaMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.disciplinaServicio = disciplinaServicio;
    }

    /**
     * Genera las asistencias diarias para un nuevo alumno (según su inscripción)
     * asociándolas a la planilla de asistencia mensual indicada.
     * Si ya existen registros para ese alumno en la planilla, se omite la generación.
     */
    @Transactional
    public void registrarAsistenciasParaNuevoAlumno(Long inscripcionId, Long planillaId) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        AsistenciaMensual planilla = asistenciaMensualRepositorio.findById(planillaId)
                .orElseThrow(() -> new IllegalArgumentException("Planilla de asistencia no encontrada"));

        // Evitar duplicados: si el alumno ya tiene asistencias en esta planilla, no se crea nada
        boolean existe = asistenciaDiariaRepositorio.existsByAlumnoIdAndAsistenciaMensualId(
                inscripcion.getAlumno().getId(), planillaId);
        if (existe) {
            log.info("El alumno ya posee asistencias registradas en la planilla id: {}", planillaId);
            return;
        }

        // Calcular los días de clase para la disciplina de la planilla
        List<LocalDate> fechasClase = disciplinaServicio.obtenerDiasClase(
                planilla.getDisciplina().getId(),
                planilla.getMes(),
                planilla.getAnio()
        );

        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE,
                        inscripcion.getAlumno(), planilla))
                .collect(Collectors.toList());
        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
    }

    @Transactional
    public AsistenciaDiariaResponse registrarAsistencia(AsistenciaDiariaRegistroRequest request) {
        validarFecha(request.fecha());
        AsistenciaMensual planilla = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la planilla de asistencia"));
        if (asistenciaDiariaRepositorio.existsByAlumnoIdAndFecha(request.alumnoId(), request.fecha())) {
            throw new IllegalStateException("Ya existe una asistencia registrada para este alumno en esta fecha.");
        }
        // Se asume que para registrar asistencia se busca el alumno a través de la inscripción
        Alumno alumno = inscripcionRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"))
                .getAlumno();
        AsistenciaDiaria asistencia = new AsistenciaDiaria();
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setAlumno(alumno);
        asistencia.setAsistenciaMensual(planilla);
        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    @Transactional
    public AsistenciaDiariaResponse actualizarAsistencia(Long id, AsistenciaDiariaModificacionRequest request) {
        AsistenciaDiaria asistencia = asistenciaDiariaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia diaria no encontrada"));
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        return asistenciaDiariaMapper.toDTO(asistenciaDiariaRepositorio.save(asistencia));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalStateException("No se puede registrar asistencia en fechas futuras.");
        }
    }

    @Transactional(readOnly = true)
    public Page<AsistenciaDiariaResponse> obtenerAsistenciasPorDisciplinaYFecha(Long disciplinaId, LocalDate fecha, Pageable pageable) {
        return asistenciaDiariaRepositorio
                .findByAsistenciaMensual_Disciplina_IdAndFecha(disciplinaId, fecha, pageable)
                .map(asistenciaDiariaMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDiariaResponse> obtenerAsistenciasPorPlanilla(Long planillaId) {
        return asistenciaDiariaRepositorio.findByAsistenciaMensualId(planillaId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Registrar o actualizar asistencia: si ya existe para la fecha y alumno, se actualiza; si no, se crea.
     */
    @Transactional
    public AsistenciaDiariaResponse registrarOActualizarAsistencia(AsistenciaDiariaRegistroRequest request) {
        Optional<AsistenciaDiaria> asistenciaExistente = asistenciaDiariaRepositorio
                .findByAlumnoIdAndFecha(request.alumnoId(), request.fecha());
        AsistenciaDiaria asistencia = asistenciaExistente.orElseGet(AsistenciaDiaria::new);
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        AsistenciaMensual planilla = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la planilla de asistencia"));
        Alumno alumno = inscripcionRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"))
                .getAlumno();
        asistencia.setAlumno(alumno);
        asistencia.setAsistenciaMensual(planilla);
        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDiariaResponse> obtenerAsistenciasPorAsistenciaMensual(Long asistenciaMensualId) {
        return asistenciaDiariaRepositorio.findByAsistenciaMensualId(asistenciaMensualId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void eliminarAsistencia(Long id) {
        asistenciaDiariaRepositorio.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerResumenAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return asistenciaDiariaRepositorio.contarAsistenciasPorAlumno(disciplinaId, fechaInicio, fechaFin);
    }
}
