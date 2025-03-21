package ledance.dto.pago.response;

import ledance.entidades.TipoDetallePago;

import java.time.LocalDate;

public record DetallePagoResponse(
        Long id,
        String descripcionConcepto,
        String cuotaOCantidad,
        Double valorBase,
        Long bonificacionId,
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
        LocalDate fechaRegistro
) {}
