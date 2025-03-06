package ledance.dto.asistencia.response;

public record AsistenciaMensualListadoResponse(
        Long id,
        Integer mes,
        Integer anio,
        DisciplinaResponse disciplina,
        String profesor,
        Integer cantidadAlumnos
) { }
