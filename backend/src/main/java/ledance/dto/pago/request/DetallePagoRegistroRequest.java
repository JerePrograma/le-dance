package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePagoRegistroRequest(
        Long id,
        String descripcionConcepto,
        String cuotaOCantidad,
        @NotNull @Min(0) Double valorBase,
        Long bonificacionId,
        Long recargoId,
        @NotNull @Min(0) Double aCobrar,
        Boolean cobrado,
        Long conceptoId,
        Long subConceptoId,
        Long mensualidadId,
        Long matriculaId,
        Long stockId
) {
    public DetallePagoRegistroRequest {
        // Asignamos false por defecto si viene null
        if (cobrado == null) {
            cobrado = false;
        }
    }
}
