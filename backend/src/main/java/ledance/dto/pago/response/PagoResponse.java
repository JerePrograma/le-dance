package ledance.dto.pago.response;

import java.time.LocalDate;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        String metodoPago, // Texto legible del método de pago
        Boolean recargoAplicado,
        Double bonificacionAplicada, // Cambiado de Boolean a Double
        Double saldoRestante,
        Boolean activo,
        Long inscripcionId
) {
}
