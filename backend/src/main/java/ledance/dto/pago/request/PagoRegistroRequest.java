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
        Boolean recargoAplicado,   // Indica si se aplico el recargo global
        Boolean bonificacionAplicada, // Flag que indica si se aplico la bonificacion global
        @NotNull @Min(0) Double saldoRestante,
        Boolean pagoMatricula,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios  // Puede venir vacio en el registro inicial
) { }
