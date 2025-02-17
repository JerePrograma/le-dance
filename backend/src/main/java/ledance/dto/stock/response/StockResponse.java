package ledance.dto.stock.response;

public record StockResponse(
        Long id,
        String nombre,
        Double precio,
        TipoStockResponse tipo,    // Se incluye el objeto de tipo TipoStockResponse
        Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        Boolean activo
) { }
