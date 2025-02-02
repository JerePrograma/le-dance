package ledance.servicios;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import ledance.dto.mappers.DisciplinaMapper;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisciplinaServicio implements IDisciplinaServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaServicio.class);

    private final DisciplinaRepositorio disciplinaRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaMapper disciplinaMapper;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio,
                              DisciplinaMapper disciplinaMapper) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaMapper = disciplinaMapper;
    }

    @Override
    @Transactional
    public DisciplinaResponse crearDisciplina(DisciplinaRequest requestDTO) {
        log.info("Creando disciplina: {}", requestDTO.nombre());
        if (disciplinaRepositorio.existsByNombre(requestDTO.nombre())) {
            throw new IllegalArgumentException("Ya existe una disciplina con el mismo nombre.");
        }
        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        // AquI puedes agregar validaciones adicionales de horario si lo deseas.
        Disciplina disciplina = disciplinaMapper.toEntity(requestDTO);
        disciplina.setProfesor(profesor);
        Disciplina guardada = disciplinaRepositorio.save(disciplina);
        return disciplinaMapper.toDTO(guardada);
    }

    @Override
    public List<DisciplinaResponse> listarDisciplinas() {
        return disciplinaRepositorio.findAll().stream()
                .map(disciplinaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DisciplinaResponse obtenerDisciplinaPorId(Long id) {
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        return disciplinaMapper.toDTO(disciplina);
    }

    @Override
    @Transactional
    public DisciplinaResponse actualizarDisciplina(Long id, DisciplinaRequest requestDTO) {
        log.info("Actualizando disciplina con id: {}", id);
        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        Profesor profesor = profesorRepositorio.findById(requestDTO.profesorId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        // Se puede utilizar el mapper para crear una nueva entidad, pero luego es necesario mantener el id
        Disciplina disciplinaActualizada = disciplinaMapper.toEntity(requestDTO);
        disciplinaActualizada.setId(id);
        disciplinaActualizada.setProfesor(profesor);
        Disciplina guardada = disciplinaRepositorio.save(disciplinaActualizada);
        return disciplinaMapper.toDTO(guardada);
    }

    @Override
    @Transactional
    public void eliminarDisciplina(Long id) {
        if (!disciplinaRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Disciplina no encontrada.");
        }
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
    }

    @Override
    public List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha) {
        return disciplinaRepositorio.findAll().stream()
                .filter(d -> {
                    LocalDate targetDate = LocalDate.parse(fecha);
                    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
                    return d.getHorario() != null && d.getHorario().toLowerCase().contains(dayOfWeek.name().toLowerCase());
                })
                .map(disciplinaMapper::toDTO)
                .collect(Collectors.toList());
    }
}
