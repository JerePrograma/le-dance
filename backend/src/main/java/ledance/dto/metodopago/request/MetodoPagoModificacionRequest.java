package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para modificar un método de pago existente.
 * - `activo` ahora se puede modificar.
 */
public record MetodoPagoModificacionRequest(
        @NotBlank String descripcion, // Nombre del método de pago
        @NotNull Boolean activo // ✅ Ahora se puede modificar el estado
) {}
