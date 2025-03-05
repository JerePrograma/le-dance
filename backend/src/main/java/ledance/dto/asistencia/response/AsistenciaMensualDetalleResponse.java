package ledance.dto.asistencia.response;

import ledance.dto.alumno.response.AlumnoResumenResponse;
import java.util.List;

public record AsistenciaMensualDetalleResponse(
        Long id,
        Integer mes,
        Integer anio,
        String disciplina,           // Nombre de la disciplina
        String profesor,             // Nombre del profesor
        Long disciplinaId,           // ID de la disciplina
        List<AsistenciaDiariaResponse> asistenciasDiarias,
        String observacion,
        List<AlumnoResumenResponse> alumnos, // Alumnos incorporados (derivados, por ejemplo, de la relación en Disciplina o de las asistencias diarias)
        Integer totalClases          // Puede ser, por ejemplo, el total de días de clase en la planilla
) {}
