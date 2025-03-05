package ledance.dto.rol.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registrar un nuevo rol.
 */
public record RolRegistroRequest(
        @NotBlank String descripcion
) {}
