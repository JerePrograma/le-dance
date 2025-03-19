package ledance.dto.caja;

import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Egreso;
import ledance.entidades.Pago;

import java.util.List;

public record RendicionDTO(List<PagoResponse> pagos, List<EgresoResponse> egresos, double totalEfectivo, double totalDebito,
                           double totalEgreso) {
}
