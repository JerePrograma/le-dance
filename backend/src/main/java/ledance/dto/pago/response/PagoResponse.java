package ledance.dto.pago.response;

import java.time.LocalDate;
import java.util.List;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        String metodoPago,               // Descripción del método de pago
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        Double saldoRestante,
        Double saldoAFavor,
        Boolean activo,
        String estadoPago,
        Long inscripcionId,
        Long alumnoId,                   // Derivado de la inscripción
        String observaciones,
        List<DetallePagoResponse> detallePagos,
        List<PagoMedioResponse> pagoMedios,
        String tipoPago                // Nuevo campo: "SUBSCRIPTION" o "GENERAL"
) { }
