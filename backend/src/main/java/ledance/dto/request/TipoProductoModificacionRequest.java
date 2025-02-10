package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TipoProductoModificacionRequest(
        @NotBlank String descripcion, // ✅ Nueva descripción del tipo de producto.
        @NotNull Boolean activo // ✅ Permite activar/desactivar el tipo de producto.
) {}
