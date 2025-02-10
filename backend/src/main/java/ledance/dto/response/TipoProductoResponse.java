package ledance.dto.response;

public record TipoProductoResponse(
        Long id, // ✅ Identificador único.
        String descripcion, // ✅ Nombre del tipo de producto.
        Boolean activo // ✅ Estado del tipo de producto (activo o no).
) {}
