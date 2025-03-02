package ledance.dto.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para modificar un usuario existente.
 */
public record UsuarioModificacionRequest(
        @NotBlank String nombreUsuario,
        @NotNull Boolean activo // ✅ Permite activar o desactivar el usuario
) {}
