package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRegistroRequest(
        @NotNull @NotBlank String email,
        @NotNull @NotBlank String nombreUsuario,
        @NotNull @NotBlank String contrasena,
        @NotNull @NotBlank String rol
) {}
