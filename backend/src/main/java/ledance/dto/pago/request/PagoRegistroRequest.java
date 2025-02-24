package ledance.dto.pago.request;

// ledance/dto/pago/request/PagoRegistroRequest.java
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
        Boolean recargoAplicado,
        Double bonificacionAplicada,
        @NotNull Double saldoRestante,
        Boolean pagoMatricula,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios
) { }
