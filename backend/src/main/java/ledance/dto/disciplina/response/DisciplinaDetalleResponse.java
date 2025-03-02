package ledance.dto.disciplina.response;

import java.util.List;

public record DisciplinaDetalleResponse(
        Long id,
        String nombre,
        String salon,
        Long salonId,
        Double valorCuota,
        Double matricula,
        String profesorNombre,
        String profesorApellido,
        Long profesorId,
        Integer inscritos,
        Boolean activo,
        Double claseSuelta,
        Double clasePrueba,
        List<DisciplinaHorarioResponse> horarios
) {}
