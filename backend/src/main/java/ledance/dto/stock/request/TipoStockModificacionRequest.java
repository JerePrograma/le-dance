package ledance.dto.stock.request;

import jakarta.validation.constraints.NotNull;

public record TipoStockModificacionRequest(
        @NotNull String descripcion,
        @NotNull Boolean activo
) { }
