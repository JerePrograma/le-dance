package ledance.dto.request;

public record ProfesorRegistroRequest(
        String nombre,
        String apellido,
        String especialidad,
        Boolean activo // ✅ Opcional si quieres permitir que el usuario defina el estado activo
) {}
