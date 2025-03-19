package ledance.dto.egreso.response;

import ledance.dto.metodopago.response.MetodoPagoResponse;
import java.time.LocalDate;

public record EgresoResponse(
        Long id,
        LocalDate fecha,
        Double monto,
        String observaciones,
        MetodoPagoResponse metodoPago,
        Boolean activo
) { }
