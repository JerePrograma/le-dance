package ledance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Petición para modificar una asistencia mensual.
 * - Solo permite cambiar observaciones de alumnos.
 */
public record AsistenciaMensualModificacionRequest(
        List<AsistenciaDiariaModificacionRequest> asistenciasDiarias,// ✅ Se pueden modificar múltiples asistencias a la vez
        Map<Long, String> observacionesAlumnos // ✅ Se mantiene la estructura anterior
) {}
