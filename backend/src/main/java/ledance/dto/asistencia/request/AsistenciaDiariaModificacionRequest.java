package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaModificacionRequest(
        @NotNull Long id, // ✅ Necesario para identificar la asistencia
        LocalDate fecha, // ✅ Opcional, si no se manda no se actualiza
        @NotNull EstadoAsistencia estado,
        Long disciplinaHorarioId, // ✅ Permite reasignar la asistencia a otro horario
        String observacion
) {}
