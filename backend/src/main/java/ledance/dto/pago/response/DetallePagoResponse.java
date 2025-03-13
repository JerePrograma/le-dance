package ledance.dto.pago.response;

public record DetallePagoResponse(
        Long id,
        String codigoConcepto,
        String concepto,
        String cuota,
        Double valorBase,
        Double aFavor,
        // Se conserva el importe calculado (aunque ahora se entiende que es el importePendiente)
        Double importe,
        // Nuevo campo: importe inicial, que representa el monto total antes de abonos.
        Double importeInicial,
        // Nuevo campo: importe pendiente, que se actualiza a medida que se realizan abonos.
        Double importePendiente,
        Double aCobrar,
        Long bonificacionId,
        Long recargoId,
        Boolean cobrado
) { }
