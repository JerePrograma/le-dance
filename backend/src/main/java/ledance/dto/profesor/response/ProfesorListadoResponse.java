package ledance.dto.profesor.response;

public record ProfesorListadoResponse(
        Long id,
        String nombre,
        String apellido,
        Boolean activo
) {
}