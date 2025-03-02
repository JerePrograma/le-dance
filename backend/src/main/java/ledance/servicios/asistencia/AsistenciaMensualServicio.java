package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import ledance.entidades.ObservacionMensual;
import ledance.entidades.EstadoInscripcion;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.AlumnoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
            AsistenciaMensualMapper asistenciaMensualMapper,
            AlumnoRepositorio alumnoRepositorio,
            AsistenciaDiariaServicio asistenciaDiariaServicio) {
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.asistenciaMensualMapper = asistenciaMensualMapper;
        this.alumnoRepositorio = alumnoRepositorio;
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
    }

    /**
     * Registra una nueva asistencia mensual para una inscripción y genera las asistencias diarias correspondientes.
     */
    @Transactional
    public AsistenciaMensualDetalleResponse registrarAsistenciaMensual(AsistenciaMensualRegistroRequest request) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la inscripción con ID: " + request.inscripcionId()));

        // Aquí se puede validar que el mes tenga clases, usando la configuración de la disciplina.

        AsistenciaMensual asistenciaMensual = new AsistenciaMensual();
        asistenciaMensual.setMes(request.mes());
        asistenciaMensual.setAnio(request.anio());
        asistenciaMensual.setInscripcion(inscripcion);
        asistenciaMensual = asistenciaMensualRepositorio.save(asistenciaMensual);

        // Se generan las asistencias diarias para la inscripción (la lógica real puede calcular los días de clase)
        asistenciaDiariaServicio.registrarAsistenciasParaNuevoAlumno(inscripcion.getId());

        return asistenciaMensualMapper.toDetalleDTO(asistenciaMensual);
    }

    /**
     * Actualiza una asistencia mensual, incluyendo la actualización de observaciones.
     */
    @Transactional
    public AsistenciaMensualDetalleResponse actualizarAsistenciaMensual(Long id, AsistenciaMensualModificacionRequest request) {
        AsistenciaMensual existente = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe AsistenciaMensual con ID: " + id));

        // Actualización centralizada de observaciones
        actualizarObservaciones(existente, request.observacionesAlumnos());

        // Actualiza los campos modificables mediante el mapper (los campos críticos se ignoran)
        asistenciaMensualMapper.updateEntityFromRequest(request, existente);
        AsistenciaMensual actualizada = asistenciaMensualRepositorio.save(existente);
        return asistenciaMensualMapper.toDetalleDTO(actualizada);
    }

    /**
     * Método auxiliar para actualizar las observaciones.
     */
    private void actualizarObservaciones(AsistenciaMensual asistenciaMensual, Map<Long, String> observacionesMap) {
        if (observacionesMap == null) return;
        for (Map.Entry<Long, String> entry : observacionesMap.entrySet()) {
            Long alumnoId = entry.getKey();
            String texto = entry.getValue();
            Optional<ObservacionMensual> optObs = asistenciaMensual.getObservaciones().stream()
                    .filter(obs -> obs.getAlumno().getId().equals(alumnoId))
                    .findFirst();
            ObservacionMensual observacion;
            if (optObs.isPresent()) {
                observacion = optObs.get();
            } else {
                observacion = new ObservacionMensual();
                observacion.setAsistenciaMensual(asistenciaMensual);
                Alumno alumno = alumnoRepositorio.findById(alumnoId)
                        .orElseThrow(() -> new IllegalArgumentException("No existe Alumno con ID: " + alumnoId));
                observacion.setAlumno(alumno);
                asistenciaMensual.getObservaciones().add(observacion);
            }
            observacion.setObservacion(texto);
        }
    }

    /**
     * Recupera las asistencias mensuales filtradas por profesor, disciplina, mes y año.
     */
    @Transactional(readOnly = true)
    public List<AsistenciaMensualListadoResponse> listarAsistenciasMensuales(Long profesorId, Long disciplinaId, Integer mes, Integer anio) {
        List<AsistenciaMensual> asistencias = asistenciaMensualRepositorio.buscarAsistencias(profesorId, disciplinaId, mes, anio);
        return asistencias.stream()
                .map(asistenciaMensualMapper::toListadoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea asistencias mensuales automáticamente para todas las inscripciones activas de cada disciplina.
     */
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

    /**
     * Recupera la asistencia mensual para una disciplina (a través de la inscripción) para un mes y año determinados.
     */
    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerAsistenciaMensualPorParametros(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> asistenciaExistente = asistenciaMensualRepositorio
                .findByInscripcion_Disciplina_IdAndMesAndAnio(disciplinaId, mes, anio)
                .stream()
                .findFirst();
        return asistenciaExistente
                .map(asistenciaMensualMapper::toDetalleDTO)
                .orElseThrow(() -> new NoSuchElementException("No se encontró asistencia mensual para los parámetros especificados (Disciplina ID: " + disciplinaId + ", mes: " + mes + ", anio: " + anio + ")"));
    }

    /**
     * Crea asistencia mensual para la primera inscripción activa de una disciplina dada.
     */
    @Transactional
    public AsistenciaMensualDetalleResponse crearAsistenciaPorDisciplina(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> asistenciaExistente = asistenciaMensualRepositorio
                .findByInscripcion_Disciplina_IdAndMesAndAnio(disciplinaId, mes, anio)
                .stream()
                .findFirst();
        if (asistenciaExistente.isPresent()) {
            throw new IllegalStateException("Ya existe una asistencia mensual para los parámetros especificados.");
        }
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findAllByDisciplinaIdAndEstado(disciplinaId, EstadoInscripcion.ACTIVA);
        if (inscripcionesActivas.isEmpty()) {
            throw new IllegalArgumentException("No existen inscripciones activas para la disciplina con ID: " + disciplinaId);
        }
        // Para este ejemplo, se crea la asistencia para la primera inscripción activa
        AsistenciaMensual nuevaAsistencia = new AsistenciaMensual();
        nuevaAsistencia.setMes(mes);
        nuevaAsistencia.setAnio(anio);
        nuevaAsistencia.setInscripcion(inscripcionesActivas.get(0));
        nuevaAsistencia = asistenciaMensualRepositorio.save(nuevaAsistencia);
        // Se generan las asistencias diarias correspondientes para esa inscripción
        asistenciaDiariaServicio.registrarAsistenciasParaNuevoAlumno(inscripcionesActivas.get(0).getId());
        return asistenciaMensualMapper.toDetalleDTO(nuevaAsistencia);
    }
}
