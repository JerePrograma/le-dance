package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para registrar un pago.
 * Si inscripcionId es -1 se interpreta que es un pago GENERAL (sin inscripción).
 */
public record PagoRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        Long inscripcionId,  // Si es -1, se entiende que es un pago general.
        Long metodoPagoId,   // Opcional
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        Boolean pagoMatricula,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios
) { }
