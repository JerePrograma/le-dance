package ledance.dto.usuario.response;

/**
 * DTO para responder con la informacion de un usuario.
 */
public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String rolDescripcion, // ✅ Se mapea desde `rol.descripcion`
        Boolean activo
) {
}