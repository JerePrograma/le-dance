package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaRegistroRequest(
        Long id, // Para creacion o actualizacion
        @NotNull LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        Long asistenciaAlumnoMensualId
) {}
