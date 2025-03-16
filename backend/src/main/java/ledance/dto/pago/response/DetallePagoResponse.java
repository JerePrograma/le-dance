package ledance.dto.pago.response;

public record DetallePagoResponse(
        Long id,
        String codigoConcepto,
        String concepto,
        String cuota,
        // Renombrado: valorBase -> montoOriginal.
        Double montoOriginal,
        Double aFavor,
        // Se conserva el campo 'importe' que puede representar el importe total calculado.
        Double importe,
        // Nuevo campo: importe inicial, representa el monto antes de abonos.
        Double importeInicial,
        // Nuevo campo: importe pendiente, se actualiza conforme se realizan abonos.
        Double importePendiente,
        Double aCobrar,
        Long bonificacionId,
        Long recargoId,
        Boolean cobrado
) { }
