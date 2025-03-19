package ledance.dto.deudas;

import ledance.dto.pago.response.DetallePagoResponse;
import java.util.List;

public record DeudasPendientesResponse(
        Long alumnoId,
        String alumnoNombre,
        List<DetallePagoResponse> detallePagosPendientes,
        Double totalDeuda
) { }
