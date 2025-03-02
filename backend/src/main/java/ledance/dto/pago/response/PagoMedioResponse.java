package ledance.dto.pago.response;

public record PagoMedioResponse(
        Long id,
        Double monto,
        Long metodoPagoId // Referencia al metodo de pago utilizado en el abono
) { }
