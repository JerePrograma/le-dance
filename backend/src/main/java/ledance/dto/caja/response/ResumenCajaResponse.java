package ledance.dto.caja.response;

import java.time.LocalDate;
import java.util.List;

public record ResumenCajaResponse(
        LocalDate desde,
        LocalDate hasta,
        String totalIngresos,
        String totalEgresos,
        String saldo,
        List<MovimientoCajaResponse> movimientos
) {
}
