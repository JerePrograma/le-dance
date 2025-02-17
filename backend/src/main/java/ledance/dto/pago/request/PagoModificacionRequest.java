package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PagoModificacionRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        Long metodoPagoId, // Opcional
        Boolean recargoAplicado,
        Double bonificacionAplicada,
        @NotNull @Min(0) Double saldoRestante,
        @NotNull Boolean activo
) { }
