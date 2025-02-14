package ledance.dto.asistencia.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AsistenciaDiariaLoteRequest(
        @NotNull List<AsistenciaDiariaModificacionRequest> asistencias
) {}
