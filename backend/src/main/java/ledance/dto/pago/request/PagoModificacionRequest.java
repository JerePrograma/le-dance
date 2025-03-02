package ledance.dto.pago.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record PagoModificacionRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        Long metodoPagoId, // Opcional
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        @NotNull Boolean activo, // Se exige que no sea nulo
        List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios
) { }
