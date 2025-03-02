package ledance.dto.pago.response;

public record DetallePagoResponse(
        Long id,
        String codigoConcepto,
        String concepto,
        String cuota,
        Double valorBase,
        Double aFavor,
        Double importe,    // Se calculara dinamicamente
        Double aCobrar,
        Long bonificacionId,
        Long recargoId
) {
    public DetallePagoResponse(Long id, String codigoConcepto, String concepto, String cuota,
                               Double valorBase, Double aFavor, Double aCobrar,
                               Long bonificacionId, Long recargoId) {
        this(id, codigoConcepto, concepto, cuota, valorBase, aFavor,
                (valorBase != null && aCobrar != null) ? (valorBase - aCobrar) : valorBase,
                aCobrar, bonificacionId, recargoId);
    }
}
