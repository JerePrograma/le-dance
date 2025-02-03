package ledance.dto.response;

public record AlumnoListadoResponse(
        Long id,
        String nombre,
        String apellido,
        Boolean activo // ✅ Campo agregado correctamente
) {}
