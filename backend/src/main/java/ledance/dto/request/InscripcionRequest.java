package ledance.dto.request;

/**
 * DTO para crear/actualizar una inscripcion.
 * Campos mínimos:
 * - alumnoId
 * - disciplinaId
 * - bonificacionId (opcional)
 * - costoParticular, notas
 */
public record InscripcionRequest(
        Long alumnoId,
        Long disciplinaId,
        Long bonificacionId, // nullable
        Double costoParticular,
        String notas
) {}
