package ledance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AsistenciaDiariaLoteRequest(
        @NotNull List<AsistenciaDiariaModificacionRequest> asistencias
) {}
