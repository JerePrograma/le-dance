package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Peticion para modificar una inscripcion de un alumno en una disciplina.
 */
public record InscripcionModificacionRequest(
        @NotNull Long alumnoId,
        @NotNull Long disciplinaId,
        Long bonificacionId, // Opcional
        LocalDate fechaBaja, // ✅ Permite dar de baja una inscripcion
        Double costoParticular,
        String notas
) {}
