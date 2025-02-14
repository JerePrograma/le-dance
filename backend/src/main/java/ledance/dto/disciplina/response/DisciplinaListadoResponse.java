package ledance.dto.disciplina.response;

import java.time.LocalTime;

public record DisciplinaListadoResponse(
        Long id,
        String nombre,
        LocalTime horarioInicio,
        Boolean activo,
        Long profesorId,       // Nuevo campo para el id del profesor
        String profesorNombre  // Nuevo campo para el nombre del profesor
) {
}