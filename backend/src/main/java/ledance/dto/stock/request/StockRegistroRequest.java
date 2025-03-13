package ledance.dto.stock.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StockRegistroRequest(
        @NotNull String nombre,
        @NotNull Double precio,
        @NotNull @Min(0) Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        @NotNull LocalDate fechaIngreso,
        LocalDate fechaEgreso
        // Este campo es opcional; si se proporciona, se espera un valor del enum TipoEgreso.
) {
}
