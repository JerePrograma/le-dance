package ledance.dto.caja;

import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.PagoResponse;

import java.util.List;

public record CajaDiariaFlexDTO(
        List<PagoResponse> pagos,
        List<EgresoResponse> egresos,
        List<MetodoTotalDTO> ingresosPorMetodo,
        double totalIngresos,
        List<MetodoTotalDTO> egresosPorMetodo,
        double totalEgresos,
        double neto
) {
}