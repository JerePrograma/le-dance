package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.dto.asistencia.AsistenciaDiariaMapper;
import ledance.entidades.*;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.disciplina.DisciplinaHorarioServicio;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DisciplinaHorarioServicio.class);

    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final AsistenciaDiariaMapper asistenciaDiariaMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final DisciplinaServicio disciplinaServicio;

    public AsistenciaDiariaServicio(
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            AsistenciaDiariaMapper asistenciaDiariaMapper,
            InscripcionRepositorio inscripcionRepositorio,
            AsistenciaMensualRepositorio asistenciaMensualRepositorio, DisciplinaServicio disciplinaServicio) {
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaDiariaMapper = asistenciaDiariaMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.disciplinaServicio = disciplinaServicio;
    }

    /**
     * Registrar asistencia diaria para un alumno en un día específico.
     * Se valida que la fecha no sea futura y, opcionalmente, que sea un día de clase (podrías agregar esa validación).
     */
    @Transactional
    public AsistenciaDiariaResponse registrarAsistencia(AsistenciaDiariaRegistroRequest request) {
        validarFecha(request.fecha());
        // Aquí podrías validar si el día es un día de clase de la disciplina.

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

        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    /**
     * Actualizar la asistencia diaria.
     */
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

    /**
     * Obtener asistencias diarias filtradas por disciplina (a través de asistenciaMensual) y fecha.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaDiariaResponse> obtenerAsistenciasPorDisciplinaYFecha(Long disciplinaId, LocalDate fecha, Pageable pageable) {
        return asistenciaDiariaRepositorio
                .findByAsistenciaMensual_Inscripcion_Disciplina_IdAndFecha(disciplinaId, fecha, pageable)
                .map(asistenciaDiariaMapper::toDTO);
    }

    /**
     * Registrar asistencias automáticas para un nuevo alumno.
     * Aquí se debe generar las asistencias en los días de clase según la configuración de la disciplina.
     * Este método es un ejemplo y en producción probablemente deberás calcular las fechas de clase.
     */
    /**
     * Registra las asistencias diarias para un nuevo alumno, utilizando los días de clase
     * calculados a partir de los DisciplinaHorario de la disciplina.
     */
    @Transactional
    public void registrarAsistenciasParaNuevoAlumno(Long inscripcionId) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        // Recuperamos la asistencia mensual (usando mes y año actual)
        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findByInscripcionAndMesAndAnio(
                inscripcion, LocalDate.now().getMonthValue(), LocalDate.now().getYear()
        ).orElseThrow(() -> new IllegalArgumentException("No se encontró asistencia mensual para este alumno"));

        // Si ya existen asistencias para esta asistencia mensual, no se vuelven a crear
        if (!asistenciaMensual.getAsistenciasDiarias().isEmpty()) {
            log.info("Ya existen asistencias diarias para la asistencia mensual id: {}", asistenciaMensual.getId());
            return;
        }

        // Calcula las fechas de clase para el mes
        List<LocalDate> fechasClase = disciplinaServicio.obtenerDiasClase(
                inscripcion.getDisciplina().getId(),
                asistenciaMensual.getMes(),
                asistenciaMensual.getAnio()
        );

        // Crea las asistencias para cada fecha
        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, inscripcion.getAlumno(), asistenciaMensual))
                .collect(Collectors.toList());

        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
    }

    /**
     * Eliminar asistencia diaria.
     */
    public void eliminarAsistencia(Long id) {
        asistenciaDiariaRepositorio.deleteById(id);
    }

    /**
     * Obtener el resumen de asistencias por alumno en un período.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerResumenAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return asistenciaDiariaRepositorio.contarAsistenciasPorAlumno(disciplinaId, fechaInicio, fechaFin);
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

        AsistenciaMensual asistenciaMensual = asistenciaMensualRepositorio.findById(request.asistenciaMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró asistencia mensual"));

        Alumno alumno = inscripcionRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado")).getAlumno();

        asistencia.setAlumno(alumno);
        asistencia.setAsistenciaMensual(asistenciaMensual);

        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    /**
     * Obtener asistencias diarias asociadas a una asistencia mensual.
     */
    @Transactional(readOnly = true)
    public List<AsistenciaDiariaResponse> obtenerAsistenciasPorAsistenciaMensual(Long asistenciaMensualId) {
        return asistenciaDiariaRepositorio.findByAsistenciaMensualId(asistenciaMensualId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }
}
