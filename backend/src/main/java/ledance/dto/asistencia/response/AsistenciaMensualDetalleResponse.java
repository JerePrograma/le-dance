package ledance.dto.asistencia.response;

import ledance.dto.alumno.response.AlumnoResumenResponse;
import ledance.dto.response.ObservacionMensualResponse;
import java.util.List;

public record AsistenciaMensualDetalleResponse(
        Long id,
        Integer mes,
        Integer anio,
        String disciplina, // Nombre de la disciplina
        String profesor,   // Nombre del profesor
        Long disciplinaId, // Obtenido desde inscripcion.disciplina.id
        List<AsistenciaDiariaResponse> asistenciasDiarias,
        List<ObservacionMensualResponse> observaciones,
        Long inscripcionId,
        List<AlumnoResumenResponse> alumnos,
        Integer totalClases
) {}
