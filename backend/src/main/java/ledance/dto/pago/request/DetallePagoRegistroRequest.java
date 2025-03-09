package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePagoRegistroRequest(
        Long id,
        String codigoConcepto,
        @NotNull String concepto,
        String cuota,
        @NotNull @Min(0) Double valorBase,
        @Min(0) Long bonificacionId,
        @Min(0) Long recargoId,
        @NotNull @Min(0) Double aCobrar,
        Double importe
) { }
