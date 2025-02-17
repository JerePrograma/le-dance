package ledance.dto.stock.request;

import jakarta.validation.constraints.NotNull;

public record TipoStockRegistroRequest(
        @NotNull String descripcion
) { }
