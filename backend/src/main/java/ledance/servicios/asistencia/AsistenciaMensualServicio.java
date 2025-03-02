package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.springframework.context.annotation.Lazy;
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
    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final DisciplinaServicio disciplinaServicio;

    public AsistenciaMensualServicio(
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            InscripcionRepositorio inscripcionRepositorio,
            DisciplinaRepositorio disciplinaRepositorio,
            AsistenciaMensualMapper asistenciaMensualMapper,
            AlumnoRepositorio alumnoRepositorio,
            @Lazy AsistenciaDiariaServicio asistenciaDiariaServicio,
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            @Lazy DisciplinaServicio disciplinaServicio) {  // <-- Marcar esta dependencia como @Lazy
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.asistenciaMensualMapper = asistenciaMensualMapper;
        this.alumnoRepositorio = alumnoRepositorio;
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.disciplinaServicio = disciplinaServicio;
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

    @Transactional
    public AsistenciaMensualDetalleResponse actualizarAsistenciaMensual(Long id, AsistenciaMensualModificacionRequest request) {
        AsistenciaMensual existente = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe AsistenciaMensual con ID: " + id));

        actualizarObservaciones(existente, request.observacionesAlumnos());
        asistenciaMensualMapper.updateEntityFromRequest(request, existente);
        AsistenciaMensual actualizada = asistenciaMensualRepositorio.save(existente);
        return asistenciaMensualMapper.toDetalleDTO(actualizada);
    }

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

    @Transactional(readOnly = true)
    public List<AsistenciaMensualListadoResponse> listarAsistenciasMensuales(Long profesorId, Long disciplinaId, Integer mes, Integer anio) {
        List<AsistenciaMensual> asistencias = asistenciaMensualRepositorio.buscarAsistencias(profesorId, disciplinaId, mes, anio);
        return asistencias.stream()
                .map(asistenciaMensualMapper::toListadoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Método para generar asistencias mensuales bajo demanda.
     * Se utiliza, por ejemplo, cuando se inscribe el primer alumno en la disciplina.
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
        // Se crea la asistencia para la primera inscripción activa
        AsistenciaMensual nuevaAsistencia = new AsistenciaMensual();
        nuevaAsistencia.setMes(mes);
        nuevaAsistencia.setAnio(anio);
        nuevaAsistencia.setInscripcion(inscripcionesActivas.get(0));
        nuevaAsistencia = asistenciaMensualRepositorio.save(nuevaAsistencia);
        asistenciaDiariaServicio.registrarAsistenciasParaNuevoAlumno(inscripcionesActivas.get(0).getId());
        return asistenciaMensualMapper.toDetalleDTO(nuevaAsistencia);
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

    @Transactional
    public void actualizarAsistenciasPorCambioHorario(Long disciplinaId, LocalDate fechaCambio) {
        int mes = fechaCambio.getMonthValue();
        int anio = fechaCambio.getYear();

        // Obtenemos todas las inscripciones activas de la disciplina
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findAllByDisciplinaIdAndEstado(disciplinaId, EstadoInscripcion.ACTIVA);

        for (Inscripcion inscripcion : inscripcionesActivas) {
            Optional<AsistenciaMensual> optionalAsistencia = asistenciaMensualRepositorio
                    .findByInscripcionAndMesAndAnio(inscripcion, mes, anio);
            if (optionalAsistencia.isPresent()) {
                AsistenciaMensual am = optionalAsistencia.get();

                // BORRAR de forma directa todas las asistencias diarias para este registro
                // cuya fecha sea mayor o igual a fechaCambio.
                asistenciaDiariaRepositorio.deleteByAsistenciaMensualIdAndFechaGreaterThanEqual(am.getId(), fechaCambio);

                // Recalcular las nuevas fechas de clase para el mes (filtrando desde fechaCambio)
                List<LocalDate> nuevasFechas = disciplinaServicio.obtenerDiasClase(disciplinaId, mes, anio).stream()
                        .filter(f -> !f.isBefore(fechaCambio))
                        .collect(Collectors.toList());

                // Crear las nuevas asistencias diarias
                List<AsistenciaDiaria> nuevasAsistencias = nuevasFechas.stream()
                        .map(f -> new AsistenciaDiaria(null, f, EstadoAsistencia.AUSENTE, inscripcion.getAlumno(), am, null))
                        .collect(Collectors.toList());
                asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
            }
        }
    }
}
