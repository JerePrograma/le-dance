package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record PagoRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        @NotNull Long inscripcionId,
        Long metodoPagoId,         // Opcional
        Boolean recargoAplicado,   // Indica si se aplicó el recargo global
        Boolean bonificacionAplicada, // Flag que indica si se aplicó la bonificación global
        Boolean pagoMatricula,     // Se conserva para lógica futura (aunque por ahora se omite su uso)
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios  // Puede venir vacío en el registro inicial
) { }
