package ledance.servicios;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;

import java.util.List;

public interface IDisciplinaServicio {
    DisciplinaResponse crearDisciplina(DisciplinaRequest requestDTO);
    List<DisciplinaResponse> listarDisciplinas();
    DisciplinaResponse obtenerDisciplinaPorId(Long id);
    DisciplinaResponse actualizarDisciplina(Long id, DisciplinaRequest requestDTO);
    void eliminarDisciplina(Long id);
    List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha);
}
