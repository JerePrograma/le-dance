package ledance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para modificar un producto existente.
 * - `activo` ahora se puede modificar.
 */
public record ProductoModificacionRequest(
        @NotBlank String nombre,
        @NotNull @Min(0) Double precio,
        Long tipoProductoId, // ✅ Opcional
        @NotNull @Min(0) Integer stock,
        @NotNull Boolean requiereControlDeStock,
        String codigoBarras, // ✅ Opcional
        @NotNull Boolean activo // ✅ Ahora se puede modificar el estado
) {}
