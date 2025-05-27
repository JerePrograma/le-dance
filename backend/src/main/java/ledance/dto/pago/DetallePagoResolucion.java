package ledance.dto.pago;

import ledance.entidades.TipoDetallePago;

public record DetallePagoResolucion(
        TipoDetallePago tipo,
        Long conceptoId,
        Long subConceptoId,
        Long matriculaId,
        Long mensualidadId,
        Long stockId
) {
}
