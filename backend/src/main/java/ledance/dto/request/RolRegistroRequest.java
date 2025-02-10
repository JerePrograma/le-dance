package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registrar un nuevo rol.
 */
public record RolRegistroRequest(
        @NotBlank String descripcion // ✅ Nombre del rol (ADMIN, USER, etc.)
) {}
