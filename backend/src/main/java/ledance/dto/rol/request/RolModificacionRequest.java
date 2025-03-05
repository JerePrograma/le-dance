package ledance.dto.rol.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para modificar un rol existente.
 */
public record RolModificacionRequest(
        @NotBlank String descripcion,
        @NotNull Boolean activo
) {}
