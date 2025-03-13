package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.NotNull;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;

import java.time.LocalDate;

/**
 * Peticion para modificar una inscripcion de un alumno en una disciplina.
 */
public record InscripcionModificacionRequest(
        @NotNull Long alumnoId,
        DisciplinaRegistroRequest disciplina,
        Long bonificacionId,
        LocalDate fechaBaja, // ✅ Permite dar de baja una inscripcion
        Double costoParticular
) {}
