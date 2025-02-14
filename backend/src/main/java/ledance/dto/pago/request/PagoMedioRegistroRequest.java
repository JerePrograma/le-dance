package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PagoMedioRegistroRequest(
        @NotNull @Min(0) Double monto,
        @NotNull Long metodoPagoId
) {}
