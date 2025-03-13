package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PagoMedioRegistroRequest(
        @NotNull @Min(0) Double montoAbonado,
        @NotNull Map<Long, @Min(0) Double> montosPorDetalle,
        @NotNull @Min(1) Long metodoPagoId
) { }
