package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePagoRegistroRequest(
        Long id,
        String codigoConcepto,
        @NotNull String concepto,
        String cuota,
        // Se renombra: de valorBase a montoOriginal.
        @NotNull @Min(0) Double montoOriginal,
        @Min(0) Long bonificacionId,
        @Min(0) Long recargoId,
        @NotNull @Min(0) Double aCobrar,
        // Campo opcional, se recalcula en el backend.
        Double importe,
        Boolean cobrado
) {
    public DetallePagoRegistroRequest {
        if (cobrado == null) {
            cobrado = false; // Si es null, se establece en false.
        }
    }
}
