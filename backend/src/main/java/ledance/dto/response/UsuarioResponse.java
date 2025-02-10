package ledance.dto.response;

/**
 * DTO para responder con la información de un usuario.
 */
public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String email,
        String rolDescripcion, // ✅ Se mapea desde `rol.descripcion`
        Boolean activo
) {}
