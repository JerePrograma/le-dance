package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoProductoRegistroRequest(
        @NotBlank String descripcion // ✅ Nombre del tipo de producto (Ej: "Indumentaria", "Accesorios").
) {}
