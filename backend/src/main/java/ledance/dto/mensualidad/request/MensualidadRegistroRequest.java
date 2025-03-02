package ledance.dto.mensualidad.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MensualidadRegistroRequest(
        @NotNull LocalDate fechaCuota,          // Puede venir preseleccionado con el mes actual
        @NotNull @Min(0) Double valorBase,
        Long recargoId,     // Opcional
        Long bonificacionId, // Opcional
        @NotNull Long inscripcionId
) { }
