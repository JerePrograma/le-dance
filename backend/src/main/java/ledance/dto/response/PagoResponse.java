package ledance.dto.response;

import java.time.LocalDate;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        String metodoPago, // ✅ Texto legible del método de pago
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        Double saldoRestante,
        Boolean activo,
        Long inscripcionId
) {}
