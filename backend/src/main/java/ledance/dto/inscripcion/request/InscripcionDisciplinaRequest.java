package ledance.dto.inscripcion.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO anidado dentro de `InscripcionRegistroRequest` para manejar la disciplina y bonificacion.
 */
public record InscripcionDisciplinaRequest(
        @NotNull Long disciplinaId,
        Long bonificacionId // ✅ Opcional
) {}
