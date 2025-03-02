package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ledance.entidades.EstadoAsistencia;

public record AsistenciaDiariaModificacionRequest(
        @NotNull Long id,
        LocalDate fecha,
        @NotNull EstadoAsistencia estado,
        String observacion
) {}
