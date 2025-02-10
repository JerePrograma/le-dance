package ledance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Petición para modificar un pago existente.
 * - `activo` ahora se puede modificar.
 */
public record PagoModificacionRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        Long metodoPagoId, // ✅ Opcional
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        @NotNull @Min(0) Double saldoRestante,
        @NotNull Boolean activo // ✅ Ahora se puede modificar el estado
) {}
