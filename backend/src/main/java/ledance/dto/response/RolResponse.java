package ledance.dto.response;

public record RolResponse(
        Long id,
        String descripcion,
        Boolean activo // ✅ Agregar este campo
) {}
