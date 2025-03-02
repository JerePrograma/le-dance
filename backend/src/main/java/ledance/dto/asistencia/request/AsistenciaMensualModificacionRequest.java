package ledance.dto.asistencia.request;

import java.util.List;
import java.util.Map;

/**
 * Peticion para modificar una asistencia mensual.
 * - Solo permite cambiar observaciones de alumnos.
 */
public record AsistenciaMensualModificacionRequest(
        List<AsistenciaDiariaModificacionRequest> asistenciasDiarias,// ✅ Se pueden modificar multiples asistencias a la vez
        Map<Long, String> observacionesAlumnos // ✅ Se mantiene la estructura anterior
) {}
