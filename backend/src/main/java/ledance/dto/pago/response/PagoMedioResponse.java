package ledance.dto.pago.response;

public record PagoMedioResponse(
        Long id,
        Double monto,
        Long metodoPagoId,              // Referencia al metodo de pago
        String metodoPagoDescripcion    // Descripcion del metodo (p.ej., "EFECTIVO")
) { }
