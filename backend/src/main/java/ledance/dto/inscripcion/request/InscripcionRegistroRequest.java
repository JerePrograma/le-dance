package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.PastOrPresent;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;

import java.time.LocalDate;

public record InscripcionRegistroRequest(
        Long id,
        Long alumnoId,
        DisciplinaRegistroRequest disciplina,
        Long bonificacionId,
        @PastOrPresent(message = "La fecha de inscripcion no puede ser futura")
        LocalDate fechaInscripcion
) {
}
