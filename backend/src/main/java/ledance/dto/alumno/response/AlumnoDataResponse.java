package ledance.dto.alumno.response;

import ledance.dto.pago.response.DetallePagoResponse;
import java.util.List;

public record AlumnoDataResponse(
        AlumnoResponse alumno,
        List<DetallePagoResponse> detallePagosPendientes
) {
}
