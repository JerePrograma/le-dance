package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Petición para modificar una bonificación existente.
 * - Permite cambiar "activo" (activar o desactivar la bonificación).
 */
public record BonificacionModificacionRequest(
        @NotBlank String descripcion,
        @NotNull @Positive Integer porcentajeDescuento,
        Boolean activo, // ✅ Ahora se puede modificar el estado
        String observaciones
) {}
