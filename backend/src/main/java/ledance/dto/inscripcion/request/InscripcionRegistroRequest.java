package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record InscripcionRegistroRequest(
        Long alumnoId, // 👈 Puede seguir con @NotNull si el ID del alumno es obligatorio
        InscripcionDisciplinaRequest inscripcion,

        // ✅ Dejamos que sea null y, si NO es null, validamos que no sea futura
        @PastOrPresent(message = "La fecha de inscripcion no puede ser futura")
        LocalDate fechaInscripcion,

        String notas
) {
}
