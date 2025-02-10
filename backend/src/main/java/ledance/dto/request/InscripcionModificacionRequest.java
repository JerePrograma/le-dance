package ledance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Petición para modificar una inscripción de un alumno en una disciplina.
 */
public record InscripcionModificacionRequest(
        @NotNull Long alumnoId,
        @NotNull Long disciplinaId,
        Long bonificacionId, // Opcional
        LocalDate fechaBaja, // ✅ Permite dar de baja una inscripción
        Double costoParticular,
        String notas
) {}
