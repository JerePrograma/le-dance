package ledance.dto.request;

public record BonificacionRequest(
        String descripcion,
        Integer porcentajeDescuento,
        Boolean activo,
        String observaciones
) {}
