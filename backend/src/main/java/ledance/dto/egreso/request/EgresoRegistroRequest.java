package ledance.dto.egreso.request;

import java.time.LocalDate;

public record EgresoRegistroRequest(
        Long id,
        LocalDate fecha,
        Double monto,
        String observaciones,
        Long metodoPagoId,
        String metodoPagoDescripcion
) {
}