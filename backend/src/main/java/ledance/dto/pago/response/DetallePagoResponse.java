package ledance.dto.pago.response;

import ledance.entidades.TipoDetallePago;
import java.time.LocalDate;

public record DetallePagoResponse(
        Long id,
        Long version,
        String descripcionConcepto,
        String cuotaOCantidad,
        Double valorBase,
        Long bonificacionId,
        String bonificacionNombre,
        Long recargoId,
        Double aCobrar,
        Boolean cobrado,
        Long conceptoId,
        Long subConceptoId,
        Long mensualidadId,
        Long matriculaId,
        Long stockId,
        Double importeInicial,
        Double importePendiente,
        TipoDetallePago tipo,
        LocalDate fechaRegistro,
        Long pagoId,
        String alumnoDisplay,
        Boolean tieneRecargo,
        Long usuarioId,
        String estadoPago
) { }
