package ledance.dto.pago.response;

import java.time.LocalDate;
import java.util.List;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        String metodoPago,               // Se muestra la descripción del método de pago
        Boolean recargoAplicado,
        Boolean bonificacionAplicadas,
        Double saldoRestante,
        Double saldoAFavor,
        Boolean activo,
        String estadoPago,
        Long inscripcionId,
        Long alumnoId,                   // Nuevo campo, derivado de la inscripción
        String observaciones,
        List<DetallePagoResponse> detallePagos,
        List<PagoMedioResponse> pagoMedios
) { }
