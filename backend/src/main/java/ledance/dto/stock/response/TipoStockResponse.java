package ledance.dto.stock.response;

public record TipoStockResponse(
        Long id,
        String descripcion,
        Boolean activo
) { }
