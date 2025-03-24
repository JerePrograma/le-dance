package ledance.dto.usuario.request;

/**
 * DTO para modificar un usuario existente.
 * La contraseña se podra actualizar (o dejar en blanco para mantener la actual).
 */
public record UsuarioModificacionRequest(
        String nombreUsuario,
        String contrasena, // Opcional; si es nulo o vacio, se mantiene la contraseña actual
        String rol,
        Boolean activo
) {}
