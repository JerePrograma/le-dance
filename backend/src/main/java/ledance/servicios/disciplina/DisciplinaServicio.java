package ledance.servicios.disciplina;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.entidades.DiaSemana;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DisciplinaServicio implements IDisciplinaServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaServicio.class);

    private final DisciplinaRepositorio disciplinaRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaMapper disciplinaMapper;
    private final AlumnoMapper alumnoMapper;
    private final ProfesorMapper profesorMapper;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio,
                              DisciplinaMapper disciplinaMapper,
                              AlumnoMapper alumnoMapper,
                              ProfesorMapper profesorMapper) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaMapper = disciplinaMapper;
        this.alumnoMapper = alumnoMapper;
        this.profesorMapper = profesorMapper;
    }

    /**
     * ✅ Registrar una nueva disciplina.
     */
    @Override
    @Transactional
    public DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest requestDTO) {
        log.info("Creando disciplina: {} a las {}", requestDTO.nombre(), requestDTO.horarioInicio());

        if (disciplinaRepositorio.existsByNombreAndHorarioInicio(requestDTO.nombre(), requestDTO.horarioInicio())) {
            throw new IllegalArgumentException("Ya existe una disciplina con el mismo nombre y horario.");
        }

        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));

        Disciplina disciplina = disciplinaMapper.toEntity(requestDTO);
        disciplina.setProfesor(profesor);

        Disciplina guardada = disciplinaRepositorio.save(disciplina);
        return disciplinaMapper.toDetalleResponse(guardada);
    }

    @Override
    public List<DisciplinaDetalleResponse> listarDisciplinas() {
        return disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toDetalleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaListadoResponse> listarDisciplinasSimplificadas() {
        return disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DisciplinaDetalleResponse obtenerDisciplinaPorId(Long id) {
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        return disciplinaMapper.toDetalleResponse(disciplina);
    }

    @Override
    @Transactional
    public DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO) {
        log.info("Actualizando disciplina con id: {}", id);

        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));

        // 🔹 Aplicar cambios y guardar
        disciplinaMapper.updateEntityFromRequest(requestDTO, existente);
        existente.setProfesor(profesor);
        return disciplinaMapper.toDetalleResponse(disciplinaRepositorio.save(existente));
    }

    @Override
    @Transactional
    public void eliminarDisciplina(Long id) {
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));

        log.info("Dando de baja la disciplina con id: {}", id);
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
    }

    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la disciplina con ID: " + disciplinaId));

        Map<String, DayOfWeek> traduccionDias = Map.of(
                "LUNES", DayOfWeek.MONDAY,
                "MARTES", DayOfWeek.TUESDAY,
                "MIERCOLES", DayOfWeek.WEDNESDAY,
                "JUEVES", DayOfWeek.THURSDAY,
                "VIERNES", DayOfWeek.FRIDAY,
                "SABADO", DayOfWeek.SATURDAY,
                "DOMINGO", DayOfWeek.SUNDAY
        );

        Set<DayOfWeek> diasSemana = disciplina.getDiasSemana().stream()
                .map(d -> traduccionDias.get(d.name().toUpperCase()))
                .filter(Objects::nonNull)
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


    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha) {
        try {
            // Convertir la fecha a LocalDate
            LocalDate targetDate = LocalDate.parse(fecha);
            DiaSemana diaSemana = DiaSemana.valueOf(targetDate.getDayOfWeek().name());

            // Buscar disciplinas que se imparten en ese dia de la semana
            List<Disciplina> disciplinas = disciplinaRepositorio.findDisciplinasPorDiaSemana(diaSemana);

            // Mapear a DisciplinaListadoResponse
            return disciplinas.stream()
                    .map(disciplinaMapper::toListadoResponse)
                    .collect(Collectors.toList());

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha invalido. Use 'YYYY-MM-DD'.");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El dia de la semana no es valido.");
        }
    }


    /**
     * ✅ Obtener disciplinas por horario de inicio.
     */
    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio) {
        return disciplinaRepositorio.findByHorarioInicio(horarioInicio).stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoListadoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId) {
        return disciplinaRepositorio.findAlumnosPorDisciplina(disciplinaId).stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProfesorListadoResponse obtenerProfesorDeDisciplina(Long disciplinaId) {
        Optional<Profesor> profesor = disciplinaRepositorio.findProfesorPorDisciplina(disciplinaId);

        return profesor.map(profesorMapper::toListadoResponse)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro profesor para la disciplina."));
    }
}