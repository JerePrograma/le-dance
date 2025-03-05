package ledance.dto.rol.response;

/**
 * DTO de respuesta para un rol.
 */
public record RolResponse(
        Long id,
        String descripcion,
        Boolean activo
) {}
