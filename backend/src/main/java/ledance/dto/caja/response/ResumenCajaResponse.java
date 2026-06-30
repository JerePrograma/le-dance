package ledance.dto.caja.response;

import java.time.LocalDate;
import ledance.dto.PageResponse;

public record ResumenCajaResponse(
        LocalDate desde,
        LocalDate hasta,
        String totalIngresos,
        String totalEgresos,
        String saldo,
        PageResponse<MovimientoCajaResponse> movimientos
) {
}
