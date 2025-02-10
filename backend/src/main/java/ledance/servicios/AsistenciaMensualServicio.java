package ledance.servicios;

import ledance.dto.request.AsistenciaMensualModificacionRequest;
import ledance.dto.request.AsistenciaMensualRegistroRequest;
import ledance.dto.response.AsistenciaMensualDetalleResponse;
import ledance.dto.mappers.AsistenciaMensualMapper;
import ledance.dto.response.AsistenciaMensualListadoResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaMensualServicio {

    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final AsistenciaMensualMapper asistenciaMensualMapper;
    private final AlumnoRepositorio alumnoRepositorio;
    private final AsistenciaDiariaServicio asistenciaDiariaServicio;

    public AsistenciaMensualServicio(
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            InscripcionRepositorio inscripcionRepositorio,
            DisciplinaRepositorio disciplinaRepositorio,
            AsistenciaMensualMapper asistenciaMensualMapper, AlumnoRepositorio alumnoRepositorio, AsistenciaDiariaServicio asistenciaDiariaServicio) {
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.asistenciaMensualMapper = asistenciaMensualMapper;
        this.alumnoRepositorio = alumnoRepositorio;
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
    }

    @Transactional
    public AsistenciaMensualDetalleResponse registrarAsistenciaMensual(AsistenciaMensualRegistroRequest request) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la inscripción con ID: " + request.inscripcionId()));

        AsistenciaMensual asistenciaMensual = new AsistenciaMensual();
        asistenciaMensual.setMes(request.mes());
        asistenciaMensual.setAnio(request.anio());
        asistenciaMensual.setInscripcion(inscripcion);
        asistenciaMensual = asistenciaMensualRepositorio.save(asistenciaMensual);

        asistenciaDiariaServicio.registrarAsistenciasParaNuevoAlumno(inscripcion.getId());

        return asistenciaMensualMapper.toDetalleDTO(asistenciaMensual);
    }

    // ✅ Método para generar asistencias diarias para un alumno
    private List<AsistenciaDiaria> generarAsistenciasParaAlumno(Inscripcion inscripcion, List<LocalDate> fechasClase) {
        return fechasClase.stream()
                .filter(fecha -> !fecha.isBefore(inscripcion.getFechaInscripcion())) // Respetar fecha de inscripción
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, inscripcion.getAlumno(), null, null))
                .collect(Collectors.toList());
    }

    public AsistenciaMensualDetalleResponse actualizarAsistenciaMensual(Long id, AsistenciaMensualModificacionRequest request) {
        AsistenciaMensual existente = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe AsistenciaMensual con ID: " + id));

        // Actualizar observaciones
        for (Map.Entry<Long, String> entry : request.observacionesAlumnos().entrySet()) {
            Alumno alumno = alumnoRepositorio.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("No existe Alumno con ID: " + entry.getKey()));

            ObservacionMensual observacion = existente.getObservaciones().stream()
                    .filter(obs -> obs.getAlumno().getId().equals(alumno.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        ObservacionMensual nuevaObs = new ObservacionMensual();
                        nuevaObs.setAsistenciaMensual(existente);
                        nuevaObs.setAlumno(alumno);
                        existente.getObservaciones().add(nuevaObs);
                        return nuevaObs;
                    });
            observacion.setObservacion(entry.getValue());
        }

        asistenciaMensualMapper.updateEntityFromRequest(request, existente);
        AsistenciaMensual actualizada = asistenciaMensualRepositorio.save(existente);
        return asistenciaMensualMapper.toDetalleDTO(actualizada);
    }

    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerAsistenciaMensual(Long id) {
        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe AsistenciaMensual con ID: " + id));
        return asistenciaMensualMapper.toDetalleDTO(asistenciaMensual);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaMensualListadoResponse> listarAsistenciasMensuales(Long profesorId, Long disciplinaId, Integer mes, Integer anio) {
        List<AsistenciaMensual> asistencias = asistenciaMensualRepositorio.buscarAsistencias(profesorId, disciplinaId, mes, anio);
        return asistencias.stream()
                .map(asistenciaMensualMapper::toListadoDTO)
                .toList();
    }


    public AsistenciaMensualDetalleResponse obtenerOCrearAsistenciaPorDisciplina(
            Long disciplinaId,
            Integer mes,
            Integer anio
    ) {
        // Buscar una asistencia mensual existente para esta disciplina y período
        Optional<AsistenciaMensual> asistenciaExistente = asistenciaMensualRepositorio
                .findByInscripcion_Disciplina_IdAndMesAndAnio(disciplinaId, mes, anio)
                .stream()
                .findFirst();

        if (asistenciaExistente.isPresent()) {
            return asistenciaMensualMapper.toDetalleDTO(asistenciaExistente.get());
        }

        // Si no existe, crear una nueva asistencia mensual
        AsistenciaMensual nuevaAsistencia = new AsistenciaMensual();
        nuevaAsistencia.setMes(mes);
        nuevaAsistencia.setAnio(anio);

        // Obtener todas las inscripciones activas para esta disciplina
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findAllByDisciplinaIdAndEstado(disciplinaId, EstadoInscripcion.ACTIVA);

        if (inscripcionesActivas.isEmpty()) {
            throw new IllegalArgumentException("No existen inscripciones activas para la disciplina");
        }

        // Crear una asistencia mensual para cada inscripción activa
        List<AsistenciaMensual> asistencias = inscripcionesActivas.stream()
                .map(inscripcion -> {
                    AsistenciaMensual asistencia = new AsistenciaMensual();
                    asistencia.setMes(mes);
                    asistencia.setAnio(anio);
                    asistencia.setInscripcion(inscripcion);
                    asistencia.setObservaciones(new ArrayList<>());
                    return asistenciaMensualRepositorio.save(asistencia);
                })
                .toList();

        // Retornar el detalle de la primera asistencia creada
        return asistenciaMensualMapper.toDetalleDTO(asistencias.get(0));
    }

    @Transactional
    public void crearAsistenciasMensualesAutomaticamente() {
        LocalDate now = LocalDate.now();
        int mes = now.getMonthValue();
        int anio = now.getYear();

        List<Disciplina> disciplinasActivas = disciplinaRepositorio.findByActivoTrue();

        for (Disciplina disciplina : disciplinasActivas) {
            List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findAllByDisciplinaIdAndEstado(disciplina.getId(), EstadoInscripcion.ACTIVA);

            for (Inscripcion inscripcion : inscripcionesActivas) {
                if (asistenciaMensualRepositorio.findByInscripcionAndMesAndAnio(inscripcion, mes, anio).isEmpty()) {
                    AsistenciaMensual nuevaAsistencia = new AsistenciaMensual();
                    nuevaAsistencia.setMes(mes);
                    nuevaAsistencia.setAnio(anio);
                    nuevaAsistencia.setInscripcion(inscripcion);
                    asistenciaMensualRepositorio.save(nuevaAsistencia);
                }
            }
        }
    }

}