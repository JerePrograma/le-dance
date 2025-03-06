package ledance.dto.asistencia.response;

import java.util.List;

public record AsistenciaMensualDetalleResponse(
        Long id,
        Integer mes,
        Integer anio,
        DisciplinaResponse disciplina,
        String profesor,
        List<AsistenciaAlumnoMensualDetalleResponse> alumnos
) { }
