package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para modificar un reporte existente.
 */
public record ReporteModificacionRequest(
        @NotBlank String descripcion, // ✅ Permite actualizar la descripción del reporte
        @NotNull Boolean activo // ✅ Permite activar o desactivar el reporte
) {}
