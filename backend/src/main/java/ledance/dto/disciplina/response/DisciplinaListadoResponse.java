package ledance.dto.disciplina.response;

public record DisciplinaListadoResponse(
        Long id,
        String nombre,
        Boolean activo,
        Long profesorId,
        String profesorNombre,
        Double claseSuelta,
        Double clasePrueba,
        Double valorCuota
) {}
