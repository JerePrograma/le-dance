package ledance.dto.asistencia.response;

import ledance.dto.disciplina.response.DisciplinaResponse;

public record AsistenciaMensualListadoResponse(
        Long id,
        Integer mes,
        Integer anio,
        DisciplinaResponse disciplina,
        String profesor,
        Integer cantidadAlumnos
) { }
