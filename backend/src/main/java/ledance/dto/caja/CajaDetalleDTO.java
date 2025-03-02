package ledance.dto.caja;

import ledance.entidades.Egreso;
import ledance.entidades.Pago;

import java.util.List;

public record CajaDetalleDTO(
        List<Pago> pagosDelDia,
        List<Egreso> egresosDelDia
) {}
