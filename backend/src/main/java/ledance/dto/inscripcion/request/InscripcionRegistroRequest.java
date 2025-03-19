package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.PastOrPresent;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import java.time.LocalDate;

public record InscripcionRegistroRequest(
        Long id,
        AlumnoRegistroRequest alumno,
        DisciplinaRegistroRequest disciplina,
        Long bonificacionId,
        @PastOrPresent(message = "La fecha de inscripción no puede ser futura")
        LocalDate fechaInscripcion,
        LocalDate fechaBaja, // Permite dar de baja una inscripción
        Double costoParticular
) { }
