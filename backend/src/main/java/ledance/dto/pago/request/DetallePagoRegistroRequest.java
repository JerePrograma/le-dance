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
        Double ACobrar,
        Boolean cobrado,
        Long conceptoId,
        Long subConceptoId,
        Long mensualidadId,
        Long matriculaId,
        Long stockId,
        Long pagoId,   // <-- ID del pago asociado (opcional)
        Boolean tieneRecargo,
        Double importePendiente,
        Double importeInicial,
        Double importeOriginal,
        Long usuarioId,
        String estadoPago,
        Boolean removido
) {
    public DetallePagoRegistroRequest {
        if (cobrado == null) {
            cobrado = false;
        }
    }
}
