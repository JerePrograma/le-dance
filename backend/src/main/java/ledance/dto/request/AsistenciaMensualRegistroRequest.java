package ledance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Petición para registrar una asistencia mensual.
 * - "activo" se asigna automáticamente en el servicio.
 */
public record AsistenciaMensualRegistroRequest(
        @NotNull Integer mes,
        @NotNull Integer anio,
        Long inscripcionId, // Opcional, si se quiere registrar sin alumnos
        List<AsistenciaDiariaRegistroRequest> asistenciasDiarias // Se generarán automáticamente si está vacío
) {}
