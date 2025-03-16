package ledance.dto.stock.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StockRegistroRequest(
        Long id,
        @NotNull String nombre,
        Double precio,
        Integer stock,
        Boolean requiereControlDeStock,
        Boolean activo,
        LocalDate fechaIngreso,
        LocalDate fechaEgreso
        // Este campo es opcional; si se proporciona, se espera un valor del enum TipoEgreso.
) {
}
