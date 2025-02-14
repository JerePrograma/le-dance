package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.dto.asistencia.AsistenciaDiariaMapper;
import ledance.entidades.*;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.disciplina.DisciplinaServicio;
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
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final AsistenciaDiariaMapper asistenciaDiariaMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final DisciplinaServicio disciplinaServicio;

    public AsistenciaDiariaServicio(
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            AsistenciaDiariaMapper asistenciaDiariaMapper, InscripcionRepositorio inscripcionRepositorio, DisciplinaRepositorio disciplinaRepositorio, DisciplinaServicio disciplinaServicio) {
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.asistenciaDiariaMapper = asistenciaDiariaMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaServicio = disciplinaServicio;
    }

    @Transactional
    public AsistenciaDiariaResponse registrarAsistencia(AsistenciaDiariaRegistroRequest request) {
        if (request.fecha().isAfter(LocalDate.now())) {
            throw new IllegalStateException("No se puede registrar asistencia para fechas futuras.");
        }

        if (asistenciaDiariaRepositorio.existsByAsistenciaMensualInscripcionAlumnoIdAndFecha(
                request.alumnoId(), request.fecha())) {
            throw new IllegalStateException("Ya existe una asistencia registrada para este alumno en esta fecha.");
        }

        return guardarAsistencia(request, false);
    }

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

    @Transactional
    public AsistenciaDiariaResponse registrarOActualizarAsistencia(AsistenciaDiariaRegistroRequest request) {
        return guardarAsistencia(request, true);
    }

    private AsistenciaDiariaResponse guardarAsistencia(AsistenciaDiariaRegistroRequest request, boolean actualizar) {
        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("Asistencia mensual no encontrada"));

        AsistenciaDiaria asistencia = asistenciaDiariaRepositorio
                .findByAsistenciaMensualIdAndAlumnoIdAndFecha(
                        request.asistenciaMensualId(), request.alumnoId(), request.fecha())
                .orElse(new AsistenciaDiaria());

        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setObservacion(request.observacion());
        asistencia.setAsistenciaMensual(asistenciaMensual);
        asistencia.setAlumno(asistenciaMensual.getInscripcion().getAlumno());

        return asistenciaDiariaMapper.toDTO(asistenciaDiariaRepositorio.save(asistencia));
    }


    private void validarAsistenciaDiaria(AsistenciaDiariaRegistroRequest request) {
        if (request.fecha().isAfter(LocalDate.now())) {
            throw new IllegalStateException("No se puede registrar asistencia para fechas futuras.");
        }

        boolean existeAsistencia = asistenciaDiariaRepositorio.existsByAsistenciaMensualInscripcionAlumnoIdAndFecha(
                request.alumnoId(), request.fecha());

        if (existeAsistencia) {
            throw new IllegalStateException("Ya existe una asistencia registrada para este alumno en esta fecha.");
        }
    }

    @Transactional(readOnly = true)
    public Page<AsistenciaDiariaResponse> obtenerAsistenciasPorDisciplinaYFecha(Long disciplinaId, LocalDate fecha, Pageable pageable) {
        return asistenciaDiariaRepositorio
                .findByAsistenciaMensual_Inscripcion_Disciplina_IdAndFecha(disciplinaId, fecha, pageable)
                .map(asistenciaDiariaMapper::toDTO);
    }

    public void eliminarAsistencia(Long id) {
        asistenciaDiariaRepositorio.deleteById(id);
    }

    @Transactional
    public List<AsistenciaDiariaResponse> registrarAsistenciasEnLote(List<AsistenciaDiariaRegistroRequest> requests) {
        return requests.stream()
                .map(this::registrarOActualizarAsistencia)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDiariaResponse> obtenerAsistenciasPorAsistenciaMensual(Long asistenciaMensualId) {
        return asistenciaDiariaRepositorio.findByAsistenciaMensualId(asistenciaMensualId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el resumen de asistencias por alumno para una fecha específica
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerResumenAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return asistenciaDiariaRepositorio.contarAsistenciasPorAlumno(disciplinaId, fechaInicio, fechaFin);
    }

    @Transactional
    public void registrarAsistenciasParaNuevoAlumno(Long inscripcionId) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la inscripción con ID: " + inscripcionId));

        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findByInscripcionAndMesAndAnio(
                inscripcion, LocalDate.now().getMonthValue(), LocalDate.now().getYear()
        ).orElseThrow(() -> new IllegalArgumentException("No se encontró asistencia mensual para este alumno"));

        List<LocalDate> fechasClase = disciplinaServicio.obtenerDiasClase(inscripcion.getDisciplina().getId(), asistenciaMensual.getMes(), asistenciaMensual.getAnio());

        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .filter(fecha -> !fecha.isBefore(inscripcion.getFechaInscripcion()))
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, inscripcion.getAlumno(), asistenciaMensual, null))
                .collect(Collectors.toList());

        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
    }
}