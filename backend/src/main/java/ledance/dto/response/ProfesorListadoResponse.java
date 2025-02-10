package ledance.dto.response;

public record ProfesorListadoResponse(
        Long id,
        String nombre,
        String apellido,
        Boolean activo
) {}
