package ledance.dto.pago.response;

import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;

import java.time.LocalDate;
import java.util.List;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        Double montoBasePago,
        MetodoPagoResponse metodoPago,
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        Double saldoRestante,
        Double saldoAFavor,
        Boolean activo,
        String estadoPago,
        InscripcionResponse inscripcion,
        Long alumnoId,
        String observaciones,
        List<DetallePagoResponse> detallePagos,
        List<PagoMedioResponse> pagoMedios,
        String tipoPago
) { }
