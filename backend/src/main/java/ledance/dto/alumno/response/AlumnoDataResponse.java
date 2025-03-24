// package ledance.dto.alumno.response;
package ledance.dto.alumno.response;

import ledance.dto.pago.response.DetallePagoResponse;

import java.util.List;

public record AlumnoDataResponse(
        AlumnoListadoResponse alumno,
        List<DetallePagoResponse> detallePagos
) { }
