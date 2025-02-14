package ledance.dto.producto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para registrar un nuevo producto.
 * - `activo` se asigna automáticamente en el servicio.
 */
public record ProductoRegistroRequest(
        @NotBlank String nombre,
        @NotNull @Min(0) Double precio,
        Long tipoProductoId, // ✅ Opcional
        @NotNull @Min(0) Integer stock,
        @NotNull Boolean requiereControlDeStock,
        String codigoBarras // ✅ Opcional
) {}
