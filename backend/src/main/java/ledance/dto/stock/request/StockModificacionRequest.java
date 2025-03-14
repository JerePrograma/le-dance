package ledance.dto.stock.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StockModificacionRequest(
        @NotNull String nombre,
        @NotNull Double precio,
        @NotNull @Min(0) Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        @NotNull Boolean activo,
        @NotNull LocalDate fechaIngreso,
        LocalDate fechaEgreso
) {
}
