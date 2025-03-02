package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Peticion para registrar una asistencia mensual.
 * - "activo" se asigna automaticamente en el servicio.
 */
public record AsistenciaMensualRegistroRequest(
        @NotNull Integer mes,
        @NotNull Integer anio,
        Long inscripcionId, // Opcional, si se quiere registrar sin alumnos
        List<AsistenciaDiariaRegistroRequest> asistenciasDiarias // Se generaran automaticamente si esta vacio
) {}
