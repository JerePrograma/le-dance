package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PagoRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        @NotNull Long inscripcionId,
        Long metodoPagoId,         // Opcional
        Boolean recargoAplicado,
        Double bonificacionAplicada,
        @NotNull @Min(0) Double saldoRestante
) { }
