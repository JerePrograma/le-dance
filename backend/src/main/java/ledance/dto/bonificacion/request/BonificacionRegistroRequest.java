package ledance.dto.bonificacion.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Peticion para registrar una bonificacion.
 * - "activo" se asigna automaticamente en el servicio.
 */
public record BonificacionRegistroRequest(
        @NotBlank String descripcion,
        Integer porcentajeDescuento,
        String observaciones,
        // Nuevo campo: valor fijo (opcional)
        Double valorFijo
) {}
