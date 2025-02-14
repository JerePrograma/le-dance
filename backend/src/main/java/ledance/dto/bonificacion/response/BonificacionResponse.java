package ledance.dto.bonificacion.response;

public record BonificacionResponse(
        Long id,
        String descripcion,
        Integer porcentajeDescuento,
        Boolean activo,
        String observaciones
) {
}