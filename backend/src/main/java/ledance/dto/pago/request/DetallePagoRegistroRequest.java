package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePagoRegistroRequest(
        String codigoConcepto,
        @NotNull String concepto,
        String cuota,
        @NotNull @Min(0) Double valorBase,
        @Min(0) Double bonificacion,
        @Min(0) Double recargo,
        @Min(0) Double aFavor
) {
}