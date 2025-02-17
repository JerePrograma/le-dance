package ledance.dto.stock.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRegistroRequest(
        @NotNull String nombre,
        @NotNull Double precio,
        @NotNull Long tipoStockId, // Cambiado de tipoId a tipoStockId
        @NotNull @Min(0) Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras
) { }
