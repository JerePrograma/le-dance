package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Petición para registrar una asistencia mensual.
 */
public record AsistenciaMensualRegistroRequest(
        @NotNull Integer mes,
        @NotNull Integer anio,
        @NotNull Long inscripcionId, // Se marca como obligatorio
        List<AsistenciaDiariaRegistroRequest> asistenciasDiarias // Opcional: se pueden generar automáticamente si está vacío
) {}
