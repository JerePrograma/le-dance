package ledance.dto.alumno.response;

import ledance.dto.cargo.response.CargoResponse;

import java.util.List;

public record AlumnoDataResponse(AlumnoListadoResponse alumno, List<CargoResponse> cargosPendientes) {
}
