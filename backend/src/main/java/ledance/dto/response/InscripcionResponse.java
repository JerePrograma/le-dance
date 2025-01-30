package ledance.dto.response;

public record InscripcionResponse(
        Long id,
        AlumnoListadoResponse alumno,
        DisciplinaSimpleResponse disciplina,
        BonificacionResponse bonificacion,
        Double costoParticular,
        String notas
) {}
