package ledance.dto.response;

import ledance.entidades.Rol;

public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String email,
        String rol // Cambiar a String en lugar de Rol
) {}
