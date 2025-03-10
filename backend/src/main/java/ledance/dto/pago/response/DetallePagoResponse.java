package ledance.dto.pago.response;

public record DetallePagoResponse(
        Long id,
        String codigoConcepto,
        String concepto,
        String cuota,
        Double valorBase,
        Double aFavor,
        Double importe,    // Se calculará dinámicamente
        Double aCobrar,
        Long bonificacionId,
        Long recargoId,
        Boolean cobrado
) {
}
