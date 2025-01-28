package ledance.dto.response;

public record BonificacionResponse(
        Long id,
        String descripcion,
        Integer porcentajeDescuento,
        Boolean activo,
        String observaciones
) {}
