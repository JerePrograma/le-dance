package ledance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaModificacionRequest(
        @NotNull Long id, // ✅ Agregado para identificar la asistencia
        LocalDate fecha, // ✅ Ahora opcional
        @NotNull EstadoAsistencia estado,
        String observacion
) {}
