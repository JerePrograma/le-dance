package ledance.dto.stock.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockModificacionRequest(
        @NotNull String nombre,
        @NotNull Double precio,
        @NotNull Long tipoId,          // Se utiliza el ID del TipoStock
        @NotNull @Min(0) Integer stock,
        Boolean requiereControlDeStock,
        String codigoBarras,
        @NotNull Boolean activo
) { }
