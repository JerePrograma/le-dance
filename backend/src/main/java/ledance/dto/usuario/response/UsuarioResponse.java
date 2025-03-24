package ledance.dto.usuario.response;

/**
 * DTO de respuesta para un usuario.
 */
public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String rol,      // Aqui se coloca la descripcion del rol, de forma consistente
        Boolean activo
) {}
