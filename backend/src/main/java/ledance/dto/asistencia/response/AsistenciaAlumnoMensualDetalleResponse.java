package ledance.dto.asistencia.response;

import java.util.List;

public record AsistenciaAlumnoMensualDetalleResponse(
        Long id,
        Long inscripcionId,
        String observacion,
        List<AsistenciaDiariaDetalleResponse> asistenciasDiarias
) { }
