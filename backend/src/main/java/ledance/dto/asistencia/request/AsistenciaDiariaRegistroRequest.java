package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaRegistroRequest(
        Long id, // Para creación o actualización
        @NotNull LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        Long asistenciaAlumnoMensualId
) {}
