package ledance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registrar un nuevo usuario.
 */
public record UsuarioRegistroRequest(
        @NotBlank String nombreUsuario,
        @Email @NotBlank String email,
        @NotBlank String contrasena,
        @NotBlank String rol // ✅ Nombre del rol (ADMIN, USER, etc.), se valida en el servicio
) {}
