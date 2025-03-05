package ledance.dto.asistencia.response;

public record AsistenciaMensualListadoResponse(
        Long id,
        Integer mes,
        Integer anio,
        Long disciplinaId,   // Obtenido de la relación directa
        String disciplina,   // Nombre de la disciplina
        String profesor      // Nombre del profesor asociado a la disciplina
) {}
