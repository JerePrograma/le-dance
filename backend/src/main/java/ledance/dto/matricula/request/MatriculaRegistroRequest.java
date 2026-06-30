package ledance.dto.matricula.request;

import jakarta.validation.constraints.NotNull;

public record MatriculaRegistroRequest(@NotNull Long alumnoId, @NotNull Integer anio) {
}
