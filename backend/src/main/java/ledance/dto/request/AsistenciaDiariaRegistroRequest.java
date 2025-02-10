package ledance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaRegistroRequest(
        Long id, // ✅ Ahora se puede usar tanto para creación como actualización
        @NotNull LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        @NotNull Long alumnoId,
        @NotNull Long asistenciaMensualId,
        String observacion
) {}
