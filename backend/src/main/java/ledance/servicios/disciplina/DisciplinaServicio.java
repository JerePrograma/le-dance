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
    }

    /**
     * Registrar una nueva disciplina junto con sus horarios.
     * Se mapea el request, se asigna el profesor y se persiste primero la Disciplina.
     * Luego se asigna la referencia de la disciplina a cada horario y se guardan.
     */
    @Override
    @Transactional
    public DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest request) {
        log.info("Creando disciplina: {}", request.nombre());

        // Buscar el profesor
        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));

        // Mapear el request a entidad (incluye horarios ya mapeados, pero sin la referencia al padre)
        Disciplina nuevaDisciplina = disciplinaMapper.toEntity(request);
        nuevaDisciplina.setProfesor(profesor);

        // Persistir la disciplina para generar su ID
        nuevaDisciplina = disciplinaRepositorio.save(nuevaDisciplina);

        // Si se recibieron horarios, asignar la referencia de la disciplina y guardarlos
        if (request.horarios() != null && !request.horarios().isEmpty()) {
            Disciplina finalNuevaDisciplina = nuevaDisciplina;
            List<DisciplinaHorario> horarios = request.horarios().stream()
                    .map(horarioReq -> {
                        DisciplinaHorario horario = new DisciplinaHorario();
                        horario.setDiaSemana(horarioReq.diaSemana());
                        horario.setHorarioInicio(horarioReq.horarioInicio());
                        horario.setDuracion(horarioReq.duracion());
                        horario.setDisciplina(finalNuevaDisciplina); // Asignar la disciplina
                        return horario;
                    }).collect(Collectors.toList());
            // Persistir cada horario
            disciplinaHorarioRepositorio.saveAll(horarios);
            // Actualizar la lista de horarios de la disciplina (opcional)
            nuevaDisciplina.setHorarios(horarios);
        }

        return disciplinaMapper.toDetalleResponse(nuevaDisciplina);
    }

    @Override
    @Transactional
    public void actualizarHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios) {
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);
        crearHorarios(disciplinaId, horarios);
    }

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

    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha) {
        LocalDate targetDate = LocalDate.parse(fecha);
        // Convertir el DayOfWeek a nuestro enum DiaSemana
        DiaSemana diaSemana = convertirDayOfWeekADiaSemana(targetDate.getDayOfWeek());
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDiaSemana(diaSemana);
        List<Disciplina> disciplinas = horarios.stream()
                .map(DisciplinaHorario::getDisciplina)
                .distinct()
                .toList();
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
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio) {
        // Por ahora no implementado
        return List.of();
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
                .orElseThrow(() -> new IllegalArgumentException("No se encontró profesor para la disciplina con id=" + disciplinaId));
    }

    @Override
    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
        Set<DayOfWeek> diasSemana = horarios.stream()
                .map(h -> h.getDiaSemana().toDayOfWeek())
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
    @Transactional
    public void eliminarDisciplina(Long id) {
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con id=" + id));
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
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
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con id=" + id));
        return disciplinaMapper.toDetalleResponse(disciplina);
    }

    @Override
    @Transactional
    public DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO) {
        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con id=" + id));
        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con id=" + requestDTO.profesorId()));
        // Actualizar los campos de la disciplina mediante el mapper
        disciplinaMapper.updateEntityFromRequest(requestDTO, existente);
        existente.setProfesor(profesor);
        Disciplina disciplinaActualizada = disciplinaRepositorio.save(existente);
        return disciplinaMapper.toDetalleResponse(disciplinaActualizada);
    }

    @Override
    public List<DisciplinaListadoResponse> buscarPorNombre(String nombre) {
        List<Disciplina> resultado = disciplinaRepositorio.buscarPorNombre(nombre);
        return resultado.stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
    }
}
