package ledance.dto.disciplina.response;

import java.time.LocalTime;

public record DisciplinaListadoResponse(
        Long id,
        String nombre,
        LocalTime horarioInicio,
        Boolean activo,
        Long profesorId,
        String profesorNombre,
        Double claseSuelta,  // Agregado
        Double clasePrueba,  // Agregado
        Double valorCuota   // Agregado
) {
}
