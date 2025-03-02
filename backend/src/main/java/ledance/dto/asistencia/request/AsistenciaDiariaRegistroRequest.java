package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaRegistroRequest(
        Long id, // ✅ Se puede usar para creación o actualización
        @NotNull LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        @NotNull Long alumnoId,
        @NotNull Long disciplinaHorarioId, // ✅ Se reemplaza asistenciaMensualId
        String observacion
) {}
