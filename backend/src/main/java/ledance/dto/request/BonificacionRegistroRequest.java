package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Petición para registrar una bonificación.
 * - "activo" se asigna automáticamente en el servicio.
 */
public record BonificacionRegistroRequest(
        @NotBlank String descripcion,
        @NotNull @Positive Integer porcentajeDescuento,
        String observaciones
) {}
