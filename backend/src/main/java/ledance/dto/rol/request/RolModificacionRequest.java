package ledance.dto.rol.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para modificar un rol existente.
 */
public record RolModificacionRequest(
        @NotBlank String descripcion, // ✅ Permite actualizar el nombre del rol
        @NotNull Boolean activo // ✅ Permite activar o desactivar el rol
) {}
