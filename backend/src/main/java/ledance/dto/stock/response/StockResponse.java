package ledance.dto.stock.response;

import java.time.LocalDate;

public record StockResponse(
        Long id,
        String nombre,
        Double precio,
        Integer stock,
        Boolean requiereControlDeStock,
        Boolean activo,
        LocalDate fechaIngreso,
        LocalDate fechaEgreso
) {
}
