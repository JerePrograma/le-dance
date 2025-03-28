package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ledance.dto.alumno.request.AlumnoRegistroRequest;

public record DetallePagoRegistroRequest(
        Long id,
        Long version,
        AlumnoRegistroRequest alumno,
        String descripcionConcepto,
        String cuotaOCantidad,
        @NotNull @Min(0) Double valorBase,
        Long bonificacionId,
        Long recargoId,
        Double aCobrar,
        Boolean cobrado,
        Long conceptoId,
        Long subConceptoId,
        Long mensualidadId,
        Long matriculaId,
        Long stockId,
        Long pagoId,   // <-- Nuevo campo: ID del Pago asociado (opcional)
        Boolean tieneRecargo
) {
    public DetallePagoRegistroRequest {
        if (cobrado == null) {
            cobrado = false;
        }
    }
}
