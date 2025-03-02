package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.dto.asistencia.AsistenciaDiariaMapper;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaDiariaServicio {

    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final AsistenciaDiariaMapper asistenciaDiariaMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;

    public AsistenciaDiariaServicio(
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            AsistenciaDiariaMapper asistenciaDiariaMapper,
            InscripcionRepositorio inscripcionRepositorio,
            AsistenciaMensualRepositorio asistenciaMensualRepositorio) {
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaDiariaMapper = asistenciaDiariaMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
    }

    /**
     * ✅ Registrar asistencia diaria para un alumno.
     */
    @Transactional
    public AsistenciaDiariaResponse registrarAsistencia(AsistenciaDiariaRegistroRequest request) {
        validarFecha(request.fecha());

        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la asistencia mensual"));

        if (asistenciaDiariaRepositorio.existsByAlumnoIdAndFecha(request.alumnoId(), request.fecha())) {
            throw new IllegalStateException("Ya existe una asistencia registrada para este alumno en esta fecha.");
        }

        Alumno alumno = inscripcionRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado")).getAlumno();

        AsistenciaDiaria asistencia = new AsistenciaDiaria();
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setAlumno(alumno);
        asistencia.setAsistenciaMensual(asistenciaMensual);
        asistencia.setObservacion(request.observacion());

        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    /**
     * ✅ Actualizar asistencia diaria.
     */
    @Transactional
    public AsistenciaDiariaResponse actualizarAsistencia(Long id, AsistenciaDiariaModificacionRequest request) {
        AsistenciaDiaria asistencia = asistenciaDiariaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia diaria no encontrada"));

        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());

        if (request.observacion() != null) {
            asistencia.setObservacion(request.observacion());
        }

        return asistenciaDiariaMapper.toDTO(asistenciaDiariaRepositorio.save(asistencia));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalStateException("No se puede registrar asistencia en fechas futuras.");
        }
    }

    /**
     * ✅ Obtener asistencias diarias por disciplina y fecha.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaDiariaResponse> obtenerAsistenciasPorDisciplinaYFecha(Long disciplinaId, LocalDate fecha, Pageable pageable) {
        return asistenciaDiariaRepositorio
                .findByAsistenciaMensual_Inscripcion_Disciplina_IdAndFecha(disciplinaId, fecha, pageable)
                .map(asistenciaDiariaMapper::toDTO);
    }

    /**
     * ✅ Registrar asistencias automáticas para un nuevo alumno.
     */
    @Transactional
    public void registrarAsistenciasParaNuevoAlumno(Long inscripcionId) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findByInscripcionAndMesAndAnio(
                inscripcion, LocalDate.now().getMonthValue(), LocalDate.now().getYear()
        ).orElseThrow(() -> new IllegalArgumentException("No se encontró asistencia mensual para este alumno"));

        List<LocalDate> fechasClase = List.of(LocalDate.now()); // TODO: Obtener fechas reales

        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, inscripcion.getAlumno(), asistenciaMensual, null))
                .collect(Collectors.toList());

        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
    }

    /**
     * ✅ Eliminar asistencia diaria.
     */
    public void eliminarAsistencia(Long id) {
        asistenciaDiariaRepositorio.deleteById(id);
    }

    /**
     * ✅ Obtener resumen de asistencias por alumno en un período.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerResumenAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return asistenciaDiariaRepositorio.contarAsistenciasPorAlumno(disciplinaId, fechaInicio, fechaFin);
    }

    @Transactional
    public AsistenciaDiariaResponse registrarOActualizarAsistencia(AsistenciaDiariaRegistroRequest request) {
        Optional<AsistenciaDiaria> asistenciaExistente = asistenciaDiariaRepositorio
                .findByAlumnoIdAndFecha(request.alumnoId(), request.fecha());

        AsistenciaDiaria asistencia = asistenciaExistente.orElseGet(AsistenciaDiaria::new);

        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setObservacion(request.observacion());

        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró asistencia mensual"));

        Alumno alumno = inscripcionRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado")).getAlumno();

        asistencia.setAlumno(alumno);
        asistencia.setAsistenciaMensual(asistenciaMensual);

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

}
