package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Petición para registrar un nuevo pago.
 * - `activo` se asigna automáticamente en el servicio.
 */
// Actualización de DTO para registro
public record PagoRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        @NotNull Long inscripcionId,
        Long metodoPagoId, // opcional
        Boolean recargoAplicado,
        Double bonificacionAplicada,   // ahora es Double
        @NotNull @Min(0) Double saldoRestante
) { }
