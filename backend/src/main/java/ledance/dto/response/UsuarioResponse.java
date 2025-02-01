package ledance.dto.response;

public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String email,
        String rolDescripcion
) {}
