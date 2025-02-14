package ledance.dto.rol.response;

/**
 * DTO para responder con la información de un rol.
 */
public record RolResponse(
        Long id,
        String descripcion,
        Boolean activo
) {
}