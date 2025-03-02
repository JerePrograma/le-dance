package ledance.dto.asistencia.request;

import java.util.List;
import java.util.Map;

/**
 * Petición para modificar una asistencia mensual.
 * Permite actualizar las observaciones de alumnos y modificar múltiples asistencias diarias.
 */
public record AsistenciaMensualModificacionRequest(
        List<AsistenciaDiariaModificacionRequest> asistenciasDiarias,
        Map<Long, String> observacionesAlumnos
) {}
