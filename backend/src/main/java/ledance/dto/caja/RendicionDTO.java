package ledance.dto.caja;

import ledance.entidades.Egreso;
import ledance.entidades.Pago;

import java.util.List;

public record RendicionDTO(List<Pago> pagos, List<Egreso> egresos, double totalEfectivo, double totalDebito,
                           double totalEgreso) {
}
