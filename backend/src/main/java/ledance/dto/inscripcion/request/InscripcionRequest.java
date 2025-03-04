package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record InscripcionRequest(
        @NotNull Long alumnoId,
        @NotNull Long disciplinaId,
        Long bonificacionId, // Opcional
        @PastOrPresent(message = "La fecha de inscripción no puede ser futura")
        LocalDate fechaInscripcion,
        // Campos para actualización (pueden ser null en creación)
        LocalDate fechaBaja,
        Double costoParticular
) { }
