package ledance.dto.producto.response;

public record ProductoResponse(
        Long id,
        String nombre,
        Double precio,
        String tipoProducto, // ✅ Se devuelve como `String` con la descripción del tipo
        Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        Boolean activo // ✅ Indica si el producto está activo o no
) {
}