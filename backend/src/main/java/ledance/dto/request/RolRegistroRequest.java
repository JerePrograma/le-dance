package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RolRegistroRequest(
        @NotBlank String descripcion,
        Boolean activo // ✅ Agregar este campo si es necesario
) {}
