package ledance.dto.alumno.response;

import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.dto.pago.response.PagoResponse;

import java.util.List;

public record AlumnoDataResponse(
        AlumnoDetalleResponse alumno,
        List<InscripcionResponse> inscripcionesActivas,
        DeudasPendientesResponse deudas,  // Incluye detallePagosPendientes
        PagoResponse ultimoPago
) {}
