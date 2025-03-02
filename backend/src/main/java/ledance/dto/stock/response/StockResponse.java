package ledance.dto.stock.response;

import java.time.LocalDate;

public record StockResponse(
        Long id,
        String nombre,
        Double precio,
        TipoStockResponse tipo,  // Se asume que tienes un record TipoStockResponse definido
        Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        Boolean activo,
        LocalDate fechaIngreso,
        LocalDate fechaEgreso
) {
}
