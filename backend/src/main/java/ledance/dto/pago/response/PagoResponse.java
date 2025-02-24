package ledance.dto.pago.response;

import java.time.LocalDate;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        String metodoPago,
        Boolean recargoAplicado,
        Double bonificacionAplicada,
        Double saldoRestante,
        Boolean activo,
        String estadoPago,
        Long inscripcionId,
        String observaciones
) { }
