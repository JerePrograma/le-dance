package ledance.dto.disciplina.response;

import java.time.LocalTime;

public record DisciplinaListadoResponse(
        Long id,
        String nombre,
        LocalTime horarioInicio, // ✅ Agregado
        Boolean activo,
        Long profesorId,
        String profesorNombre,
        Double claseSuelta,
        Double clasePrueba,
        Double valorCuota
) {}
