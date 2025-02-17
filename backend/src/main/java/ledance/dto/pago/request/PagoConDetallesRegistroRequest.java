package ledance.dto.pago.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record PagoConDetallesRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull Long inscripcionId,
        Boolean recargoAplicado,
        Double bonificacionAplicada,
        String observaciones,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios
) {
}