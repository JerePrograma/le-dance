package ledance.servicios.disciplina;

import ledance.dto.disciplina.DisciplinaHorarioMapper;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.repositorios.DisciplinaHorarioRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisciplinaHorarioServicio {

    private final DisciplinaHorarioRepositorio disciplinaHorarioRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final DisciplinaHorarioMapper disciplinaHorarioMapper;

    public DisciplinaHorarioServicio(
            DisciplinaHorarioRepositorio disciplinaHorarioRepositorio,
            DisciplinaRepositorio disciplinaRepositorio, DisciplinaHorarioMapper disciplinaHorarioMapper
    ) {
        this.disciplinaHorarioRepositorio = disciplinaHorarioRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.disciplinaHorarioMapper = disciplinaHorarioMapper;
    }

    /**
     * ✅ Guardar horarios para una disciplina.
     */
    @Transactional
    public List<DisciplinaHorarioResponse> guardarHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios) {
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada"));

        // Eliminar horarios previos
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);

        // Crear nuevos horarios
        List<DisciplinaHorario> nuevosHorarios = horarios.stream()
                .map(req -> {
                    DisciplinaHorario horario = disciplinaHorarioMapper.toEntity(req);
                    horario.setDisciplina(disciplina);
                    return horario;
                })
                .toList();

        return disciplinaHorarioMapper.toResponseList(disciplinaHorarioRepositorio.saveAll(nuevosHorarios));
    }

    /**
     * ✅ Obtener horarios de una disciplina.
     */
    public List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId) {
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
        return disciplinaHorarioMapper.toResponseList(horarios);
    }

    /**
     * ✅ Eliminar todos los horarios de una disciplina.
     */
    @Transactional
    public void eliminarHorarios(Long disciplinaId) {
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);
    }
}
