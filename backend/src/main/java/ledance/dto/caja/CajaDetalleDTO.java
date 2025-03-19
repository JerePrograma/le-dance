package ledance.dto.caja;

import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Egreso;
import ledance.entidades.Pago;

import java.util.List;

public record CajaDetalleDTO(
        List<PagoResponse> pagosDelDia,
        List<EgresoResponse> egresosDelDia
) {}
