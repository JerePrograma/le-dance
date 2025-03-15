package ledance.dto.pago.response;

public record PagoMedioResponse(
        Long id,
        Double monto,
        Long metodoPagoId,              // Referencia al método de pago
        String metodoPagoDescripcion    // Descripcion del método (p.ej., "EFECTIVO")
) { }
