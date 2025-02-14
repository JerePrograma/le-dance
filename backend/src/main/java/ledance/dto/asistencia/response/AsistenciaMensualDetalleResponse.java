package ledance.dto.asistencia.response;

import ledance.dto.alumno.response.AlumnoResumenResponse;
import ledance.dto.response.ObservacionMensualResponse;

import java.util.List;

public record AsistenciaMensualDetalleResponse(
        Long id,
        Integer mes,
        Integer anio,
        String disciplina,
        String profesor,
        Long disciplinaId,  // ✅ Se obtiene desde inscripción
        List<AsistenciaDiariaResponse> asistenciasDiarias,
        List<ObservacionMensualResponse> observaciones,
        Long inscripcionId,
        List<AlumnoResumenResponse> alumnos,
        Integer totalClases
) {
}