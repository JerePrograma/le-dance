package ledance.dto.alumno.response;

public record AlumnoListadoResponse(
        Long id,
        String nombre,
        String apellido,
        Boolean activo
) {
}