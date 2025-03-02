package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePagoRegistroRequest(
        Long id,  // ⚠️ Agregamos el ID para actualizaciones
        String codigoConcepto,
        @NotNull String concepto,
        String cuota,
        @NotNull @Min(0) Double valorBase,
        @Min(0) Long bonificacionId,
        @Min(0) Long recargoId,
        @Min(0) Double aFavor,
        @Min(0) Double abono,
        @Min(0) Double aCobrar
) {
    public DetallePagoRegistroRequest {
        if (aCobrar == null) {
            aCobrar = 0.0;
        }
        if (abono == null) {
            abono = 0.0;
        }
    }
}

