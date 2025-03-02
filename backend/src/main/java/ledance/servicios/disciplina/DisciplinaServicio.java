package ledance.servicios.disciplina;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.entidades.*;
import ledance.repositorios.*;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DisciplinaServicio implements IDisciplinaServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaServicio.class);

    private final DisciplinaRepositorio disciplinaRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaHorarioRepositorio disciplinaHorarioRepositorio;
    private final DisciplinaMapper disciplinaMapper;
    private final AlumnoMapper alumnoMapper;
    private final ProfesorMapper profesorMapper;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio,
                              DisciplinaHorarioRepositorio disciplinaHorarioRepositorio,
                              DisciplinaMapper disciplinaMapper,
                              AlumnoMapper alumnoMapper,
                              ProfesorMapper profesorMapper,
                              InscripcionRepositorio inscripcionRepositorio,
                              AsistenciaMensualRepositorio asistenciaMensualRepositorio) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaHorarioRepositorio = disciplinaHorarioRepositorio;
        this.disciplinaMapper = disciplinaMapper;
        this.alumnoMapper = alumnoMapper;
        this.profesorMapper = profesorMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
    }

    /**
     * ✅ Registrar una nueva disciplina junto con sus horarios individuales por día.
     */
    @Override
    @Transactional
    public DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest request) {
        log.info("Creando disciplina: {}", request.nombre());

        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));

        Disciplina nuevaDisciplina = disciplinaMapper.toEntity(request);
        nuevaDisciplina.setProfesor(profesor);

        nuevaDisciplina = disciplinaRepositorio.save(nuevaDisciplina);

        // Guardar horarios independientes
        crearHorarios(nuevaDisciplina.getId(), request.horarios());

        return disciplinaMapper.toDetalleResponse(nuevaDisciplina);
    }

    /**
     * ✅ Crear horarios individuales para una disciplina.
     */
    @Override
    @Transactional
    public void crearHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios) {
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada"));

        for (DisciplinaHorarioRequest horarioRequest : horarios) {
            DisciplinaHorario horario = new DisciplinaHorario();
            horario.setDisciplina(disciplina);
            horario.setDiaSemana(horarioRequest.diaSemana());
            horario.setHorarioInicio(horarioRequest.horarioInicio());
            horario.setDuracion(horarioRequest.duracion());

            disciplinaHorarioRepositorio.save(horario);
        }
    }

    /**
     * ✅ Actualizar horarios individuales de una disciplina.
     */
    @Override
    @Transactional
    public void actualizarHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios) {
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);
        crearHorarios(disciplinaId, horarios);
    }

    /**
     * ✅ Obtener los horarios de una disciplina específica.
     */
    @Override
    public List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId) {
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
        return horarios.stream().map(h -> new DisciplinaHorarioResponse(
                h.getId(),
                h.getDiaSemana(),
                h.getHorarioInicio(),
                h.getDuracion()
        )).collect(Collectors.toList());
    }

    /**
     * ✅ Obtener disciplinas activas según una fecha específica.
     */
    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha) {
        LocalDate targetDate = LocalDate.parse(fecha);
        DiaSemana diaSemana = convertirDayOfWeekADiaSemana(targetDate.getDayOfWeek());

        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDiaSemana(diaSemana);
        List<Disciplina> disciplinas = horarios.stream().map(DisciplinaHorario::getDisciplina).distinct().toList();

        return disciplinas.stream().map(disciplinaMapper::toListadoResponse).collect(Collectors.toList());
    }

    private DiaSemana convertirDayOfWeekADiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            default -> throw new IllegalArgumentException("Día no válido: " + dayOfWeek);
        };
    }

    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio) {
        return List.of();
    }

    @Override
    public List<AlumnoListadoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId) {
        // Recupera la lista de alumnos para la disciplina dada
        return disciplinaRepositorio.findAlumnosPorDisciplina(disciplinaId).stream()
                // Mapea cada Alumno a AlumnoListadoResponse usando tu mapper
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProfesorListadoResponse obtenerProfesorDeDisciplina(Long disciplinaId) {
        // Recupera el profesor (opcional) para la disciplina dada
        Optional<Profesor> profesor = disciplinaRepositorio.findProfesorPorDisciplina(disciplinaId);

        // Si existe, lo mapea a ProfesorListadoResponse, si no lanza excepción
        return profesor.map(profesorMapper::toListadoResponse)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró profesor para la disciplina con id=" + disciplinaId));
    }


    /**
     * ✅ Obtener los días de clase de una disciplina en un mes y año específicos.
     */
    @Override
    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
        Set<DayOfWeek> diasSemana = horarios.stream()
                .map(h -> h.getDiaSemana().toDayOfWeek()) // ✅ Conversión segura
                .collect(Collectors.toSet());

        List<LocalDate> fechas = new ArrayList<>();
        YearMonth yearMonth = YearMonth.of(anio, mes);
        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fecha = LocalDate.of(anio, mes, dia);
            if (diasSemana.contains(fecha.getDayOfWeek())) {
                fechas.add(fecha);
            }
        }
        return fechas;
    }

    /**
     * ✅ Dar de baja una disciplina.
     */
    @Override
    @Transactional
    public void eliminarDisciplina(Long id) {
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
    }

    /**
     * ✅ Listar disciplinas activas con detalles.
     */
    @Override
    public List<DisciplinaDetalleResponse> listarDisciplinas() {
        return disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toDetalleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaListadoResponse> listarDisciplinasSimplificadas() {
        // Retorna solo disciplinas activas en formato resumido (ListadoResponse)
        return disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DisciplinaDetalleResponse obtenerDisciplinaPorId(Long id) {
        // Busca la disciplina, si no existe lanza excepción
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con id=" + id));
        // Mapea a DetalleResponse para obtener toda la información
        return disciplinaMapper.toDetalleResponse(disciplina);
    }

    @Override
    @Transactional
    public DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO) {
        // Busca la disciplina existente
        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con id=" + id));

        // Verifica que el profesor exista
        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con id=" + requestDTO.profesorId()));

        // Mapea los campos del DTO a la entidad existente
        disciplinaMapper.updateEntityFromRequest(requestDTO, existente);
        existente.setProfesor(profesor);

        // Guarda los cambios en BD
        Disciplina disciplinaActualizada = disciplinaRepositorio.save(existente);
        // Devuelve la disciplina en formato detallado
        return disciplinaMapper.toDetalleResponse(disciplinaActualizada);
    }

    @Override
    public List<DisciplinaListadoResponse> buscarPorNombre(String nombre) {
        // Lógica de repositorio para buscar por nombre (ej: LIKE o match exacto)
        List<Disciplina> resultado = disciplinaRepositorio.buscarPorNombre(nombre);

        // Mapea cada entidad Disciplina a su versión de listado
        return resultado.stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
    }
}
