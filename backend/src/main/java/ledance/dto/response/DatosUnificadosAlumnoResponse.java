package ledance.dto.response;

import ledance.dto.caja.response.CobranzasDataResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;

import java.util.List;

public record DatosUnificadosAlumnoResponse(CobranzasDataResponse cobranzasData, List<InscripcionResponse> inscripcionesActivas) {

}
