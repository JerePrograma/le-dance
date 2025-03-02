package ledance.dto.bonificacion.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Peticion para modificar una bonificacion existente.
 * - Permite cambiar "activo" (activar o desactivar la bonificacion).
 */
public record BonificacionModificacionRequest(
        @NotBlank String descripcion,
        @Positive Integer porcentajeDescuento,
        Boolean activo, // ✅ Ahora se puede modificar el estado
        String observaciones,
        // Nuevo campo: valor fijo (opcional)
        @Positive Double valorFijo
) {}
