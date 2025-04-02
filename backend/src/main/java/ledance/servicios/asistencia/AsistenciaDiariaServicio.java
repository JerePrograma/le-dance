package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaDetalleResponse;
import ledance.dto.asistencia.AsistenciaDiariaMapper;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.AsistenciaAlumnoMensualRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.disciplina.DisciplinaHorarioServicio;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaDiariaServicio {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaDiariaServicio.class);

    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final AsistenciaDiariaMapper asistenciaDiariaMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio;
    private final DisciplinaHorarioServicio disciplinaHorarioServicio;

    public AsistenciaDiariaServicio(
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio,
            AsistenciaDiariaMapper asistenciaDiariaMapper,
            InscripcionRepositorio inscripcionRepositorio,
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio,
            DisciplinaHorarioServicio disciplinaHorarioServicio) {
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaDiariaMapper = asistenciaDiariaMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.asistenciaAlumnoMensualRepositorio = asistenciaAlumnoMensualRepositorio;
        this.disciplinaHorarioServicio = disciplinaHorarioServicio;
    }

    /**
     * Genera las asistencias diarias para un nuevo alumno (segun su inscripcion)
     * asociandolas al registro de asistencia mensual del alumno (AsistenciaAlumnoMensual).
     * Si ya existen registros para ese alumno en la planilla, se omite la generacion.
     */
    @Transactional
    public void registrarAsistenciasParaNuevoAlumno(Long inscripcionId, Long planillaId) {
        // Buscar inscripcion
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));

        // Buscar la planilla mensual
        AsistenciaMensual planilla = asistenciaMensualRepositorio.findById(planillaId)
                .orElseThrow(() -> new IllegalArgumentException("Planilla de asistencia no encontrada"));

        // Buscar el registro de asistencia del alumno en la planilla
        AsistenciaAlumnoMensual registroAlumno = asistenciaAlumnoMensualRepositorio
                .findByInscripcionIdAndAsistenciaMensualId(inscripcionId, planillaId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia del alumno no encontrado"));

        // Evitar duplicados: si ya existen asistencias para este registro, se omite la generacion
        boolean existe = asistenciaDiariaRepositorio.existsByAsistenciaAlumnoMensualId(registroAlumno.getId());
        if (existe) {
            log.info("El alumno ya posee asistencias registradas en el registro id: {}", registroAlumno.getId());
            return;
        }

        // Calcular los dias de clase para la disciplina de la planilla
        List<LocalDate> fechasClase = obtenerDiasClase(
                planilla.getDisciplina().getId(),
                planilla.getMes(),
                planilla.getAnio()
        );

        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, registroAlumno))
                .collect(Collectors.toList());
        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
    }

    /**
     * Registra una nueva asistencia diaria para un registro de alumno.
     * Se valida que la fecha no sea futura y que no exista ya una asistencia para esa fecha.
     * Se asume que el request contiene el identificador del registro de asistencia del alumno.
     */
    @Transactional
    public AsistenciaDiariaDetalleResponse registrarAsistencia(AsistenciaDiariaRegistroRequest request) {
        validarFecha(request.fecha());

        AsistenciaAlumnoMensual registroAlumno = asistenciaAlumnoMensualRepositorio.findById(request.asistenciaAlumnoMensualId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el registro de asistencia del alumno"));

        if (asistenciaDiariaRepositorio.existsByAsistenciaAlumnoMensualIdAndFecha(registroAlumno.getId(), request.fecha())) {
            throw new IllegalStateException("Ya existe una asistencia registrada para este alumno en esta fecha.");
        }

        AsistenciaDiaria asistencia = new AsistenciaDiaria();
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setAsistenciaAlumnoMensual(registroAlumno);
        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    /**
     * Actualiza una asistencia diaria existente.
     */
    @Transactional
    public AsistenciaDiariaDetalleResponse actualizarAsistencia(Long id, AsistenciaDiariaModificacionRequest request) {
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
     * Obtiene una pagina de asistencias diarias para una disciplina y una fecha especifica.
     * Se recorre la relacion: AsistenciaDiaria → asistenciaAlumnoMensual → asistenciaMensual → disciplina.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaDiariaDetalleResponse> obtenerAsistenciasPorDisciplinaYFecha(Long disciplinaId, LocalDate fecha, Pageable pageable) {
        return asistenciaDiariaRepositorio
                .findByAsistenciaAlumnoMensual_AsistenciaMensual_Disciplina_IdAndFecha(disciplinaId, fecha, pageable)
                .map(asistenciaDiariaMapper::toDTO);
    }

    /**
     * Obtiene las asistencias diarias asociadas a una planilla (AsistenciaMensual).
     */
    @Transactional(readOnly = true)
    public List<AsistenciaDiariaDetalleResponse> obtenerAsistenciasPorPlanilla(Long planillaId) {
        return asistenciaDiariaRepositorio.findByAsistenciaAlumnoMensual_AsistenciaMensual_Id(planillaId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Registrar o actualizar asistencia: si ya existe para la fecha en el registro del alumno, se actualiza;
     * si no, se crea una nueva.
     */
    @Transactional
    public AsistenciaDiariaDetalleResponse registrarOActualizarAsistencia(AsistenciaDiariaRegistroRequest request) {
        AsistenciaAlumnoMensual registroAlumno = asistenciaAlumnoMensualRepositorio.findById(request.asistenciaAlumnoMensualId())
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia del alumno no encontrado"));

        Optional<AsistenciaDiaria> asistenciaExistente = asistenciaDiariaRepositorio
                .findByAsistenciaAlumnoMensualIdAndFecha(registroAlumno.getId(), request.fecha());
        AsistenciaDiaria asistencia = asistenciaExistente.orElseGet(AsistenciaDiaria::new);
        asistencia.setFecha(request.fecha());
        asistencia.setEstado(request.estado());
        asistencia.setAsistenciaAlumnoMensual(registroAlumno);
        asistencia = asistenciaDiariaRepositorio.save(asistencia);
        return asistenciaDiariaMapper.toDTO(asistencia);
    }

    /**
     * Obtiene las asistencias diarias de una planilla (basado en el id de AsistenciaMensual)
     * a traves de la relacion con AsistenciaAlumnoMensual.
     */
    @Transactional(readOnly = true)
    public List<AsistenciaDiariaDetalleResponse> obtenerAsistenciasPorAsistenciaMensual(Long asistenciaMensualId) {
        return asistenciaDiariaRepositorio.findByAsistenciaAlumnoMensual_AsistenciaMensual_Id(asistenciaMensualId)
                .stream()
                .map(asistenciaDiariaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarAsistencia(Long id) {
        AsistenciaDiaria asistenciaDiaria = asistenciaDiariaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("AsistenciaDiaria no encontrada."));
        asistenciaDiariaRepositorio.delete(asistenciaDiaria);
    }

    /**
     * Obtiene un resumen (conteo) de asistencias por alumno para una disciplina entre dos fechas.
     * Se asume que el repositorio implementa la consulta necesaria haciendo join con AsistenciaAlumnoMensual.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerResumenAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return asistenciaDiariaRepositorio.contarAsistenciasPorAlumno(disciplinaId, fechaInicio, fechaFin);
    }


    @Transactional
    public void eliminarAsistenciaAlumnoMensual(Long id) {
        AsistenciaAlumnoMensual asistenciaMensual = asistenciaAlumnoMensualRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("AsistenciaAlumnoMensual no encontrada."));

        // Eliminar en bloque todas las asistencias diarias asociadas a este registro
        List<AsistenciaDiaria> asistenciasDiarias = asistenciaDiariaRepositorio.findByAsistenciaAlumnoMensualId(asistenciaMensual.getId());
        if (asistenciasDiarias != null && !asistenciasDiarias.isEmpty()) {
            // Se elimina la lista completa y se hace flush para que se sincronice en la base de datos
            asistenciaDiariaRepositorio.deleteAll(asistenciasDiarias);
            asistenciaDiariaRepositorio.flush();
        }

        // Eliminar el registro de asistencia mensual y hacer flush para propagar la eliminación
        asistenciaAlumnoMensualRepositorio.delete(asistenciaMensual);
        asistenciaAlumnoMensualRepositorio.flush();
    }

    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        log.info("Calculando dias de clase para la disciplina id: {} en {}/{}", disciplinaId, mes, anio);

        // Obtener los horarios como entidades (no DTOs)
        List<DisciplinaHorario> horarios = disciplinaHorarioServicio.obtenerHorariosEntidad(disciplinaId);

        // Convertir cada horario a un DayOfWeek utilizando el metodo toDayOfWeek() de tu enum
        Set<DayOfWeek> diasClase = horarios.stream()
                .map(h -> h.getDiaSemana().toDayOfWeek())
                .collect(Collectors.toSet());

        log.info("Dias de clase identificados: {}", diasClase);

        YearMonth yearMonth = YearMonth.of(anio, mes);
        List<LocalDate> fechasClase = new ArrayList<>();
        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fecha = LocalDate.of(anio, mes, dia);
            if (diasClase.contains(fecha.getDayOfWeek())) {
                fechasClase.add(fecha);
            }
        }
        log.info("Total de dias de clase encontrados: {}", fechasClase.size());
        return fechasClase;
    }
}