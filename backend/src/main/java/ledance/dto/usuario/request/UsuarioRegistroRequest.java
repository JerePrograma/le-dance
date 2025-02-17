package ledance.dto.usuario.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registrar un nuevo usuario.
 */
public record UsuarioRegistroRequest(
        @NotBlank String nombreUsuario,
        @NotBlank String contrasena,
        @NotBlank String rol // ✅ Nombre del rol (ADMIN, USER, etc.), se valida en el servicio
) {}
