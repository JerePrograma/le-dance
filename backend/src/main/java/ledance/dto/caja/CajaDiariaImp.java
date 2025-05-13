package ledance.dto.caja;

import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.PagoResponse;
import java.util.List;

public record CajaDiariaImp(
        List<PagoResponse> pagosDelDia,
        List<EgresoResponse> egresosDelDia,
        double totalEfectivo,
        double totalDebito,
        double totalCobrado,
        double totalEgresosEfectivo,
        double totalEgresosDebito,
        double totalEgresos,
        double totalNeto
) {}
