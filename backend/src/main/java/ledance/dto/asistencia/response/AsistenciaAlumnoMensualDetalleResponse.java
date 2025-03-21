package ledance.dto.asistencia.response;

import ledance.dto.alumno.response.AlumnoResponse;

import java.util.List;

public record AsistenciaAlumnoMensualDetalleResponse(Long id, Long inscripcionId,
                                                     AlumnoResponse alumno,
                                                     String observacion,
                                                     Long asistenciaMensualId,
                                                     List<AsistenciaDiariaDetalleResponse> asistenciasDiarias) {
}
