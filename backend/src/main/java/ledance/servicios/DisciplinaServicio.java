package ledance.servicios;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisciplinaServicio {

    private final DisciplinaRepositorio disciplinaRepositorio;
    private final ProfesorRepositorio profesorRepositorio;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
    }

    public DisciplinaResponse crearDisciplina(DisciplinaRequest requestDTO) {
        validarNombreUnico(requestDTO.nombre());

        Profesor profesor = obtenerProfesorPorId(requestDTO.profesorId());
        validarConflictoDeHorario(requestDTO.horario(), requestDTO.salon(), profesor);

        Disciplina disciplina = new Disciplina();
        disciplina.setNombre(requestDTO.nombre());
        disciplina.setHorario(requestDTO.horario());
        disciplina.setFrecuenciaSemanal(requestDTO.frecuenciaSemanal());
        disciplina.setDuracion(requestDTO.duracion());
        disciplina.setSalon(requestDTO.salon());
        disciplina.setValorCuota(requestDTO.valorCuota());
        disciplina.setMatricula(requestDTO.matricula());
        disciplina.setProfesor(profesor);

        return convertirADTO(disciplinaRepositorio.save(disciplina));
    }

    public List<DisciplinaResponse> listarDisciplinas() {
        return disciplinaRepositorio.findAll()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public DisciplinaResponse obtenerDisciplinaPorId(Long id) {
        Disciplina disciplina = obtenerDisciplinaPorIdEntidad(id);
        return convertirADTO(disciplina);
    }

    public DisciplinaResponse actualizarDisciplina(Long id, DisciplinaRequest requestDTO) {
        Disciplina disciplina = obtenerDisciplinaPorIdEntidad(id);

        Profesor profesor = obtenerProfesorPorId(requestDTO.profesorId());
        validarConflictoDeHorario(requestDTO.horario(), requestDTO.salon(), profesor);

        disciplina.setNombre(requestDTO.nombre());
        disciplina.setHorario(requestDTO.horario());
        disciplina.setFrecuenciaSemanal(requestDTO.frecuenciaSemanal());
        disciplina.setDuracion(requestDTO.duracion());
        disciplina.setSalon(requestDTO.salon());
        disciplina.setValorCuota(requestDTO.valorCuota());
        disciplina.setMatricula(requestDTO.matricula());
        disciplina.setProfesor(profesor);

        return convertirADTO(disciplinaRepositorio.save(disciplina));
    }

    public void eliminarDisciplina(Long id) {
        if (!disciplinaRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Disciplina no encontrada.");
        }
        Disciplina disciplina = obtenerDisciplinaPorIdEntidad(id);
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
    }

    public List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha) {
        return disciplinaRepositorio.findAll().stream()
                .filter(d -> validarDisciplinaPorFecha(d, fecha))
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // Métodos privados
    private void validarNombreUnico(String nombre) {
        if (disciplinaRepositorio.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe una disciplina con el mismo nombre.");
        }
    }

    private void validarConflictoDeHorario(String horario, String salon, Profesor profesor) {
        boolean conflicto = disciplinaRepositorio.findAll()
                .stream()
                .anyMatch(d -> d.getProfesor().equals(profesor)
                        && d.getHorario().equals(horario)
                        && d.getSalon().equals(salon));

        if (conflicto) {
            throw new IllegalArgumentException("Conflicto de horario o salon para el profesor.");
        }
    }

    private Disciplina obtenerDisciplinaPorIdEntidad(Long id) {
        return disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
    }

    private Profesor obtenerProfesorPorId(Long id) {
        return profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
    }

    private boolean validarDisciplinaPorFecha(Disciplina disciplina, String fecha) {
        LocalDate targetDate = LocalDate.parse(fecha);
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        return disciplina.getHorario() != null
                && disciplina.getHorario().toLowerCase().contains(dayOfWeek.name().toLowerCase());
    }

    private DisciplinaResponse convertirADTO(Disciplina d) {
        // Para "inscritos", se podría contar las inscripciones en otra capa (por ejemplo, un JOIN o un servicio).
        return new DisciplinaResponse(
                d.getId(),
                d.getNombre(),
                d.getHorario(),
                d.getFrecuenciaSemanal(),
                d.getDuracion(),
                d.getSalon(),
                d.getValorCuota(),
                d.getMatricula(),
                d.getProfesor() != null ? d.getProfesor().getId() : null,
                0 // Por ahora 0, o consultado a InscripcionRepositorio si lo deseas
        );
    }
}
