package ledance.dto.request;

public record ProfesorRegistroRequest(
        String nombre,
        String apellido,
        String especialidad,
        Integer aniosExperiencia
) {}
