package ledance.dto.usuario.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registrar un nuevo usuario.
 */
public record UsuarioRegistroRequest(
        @NotBlank String nombreUsuario,
        @NotBlank String contrasena,
        @NotBlank String rol // Se espera la descripcion del rol (por ejemplo, "ADMIN", "USER", etc.)
) {}
