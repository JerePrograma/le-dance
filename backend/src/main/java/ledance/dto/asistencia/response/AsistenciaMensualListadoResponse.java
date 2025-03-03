package ledance.dto.asistencia.response;

public record AsistenciaMensualListadoResponse(
        Long id,
        Integer mes,
        Integer anio,
        Long inscripcionId,
        String observacion,
        String disciplina,
        String profesor
) { }
