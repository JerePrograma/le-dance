package ledance.dto.request;


/**
 * Representa UNA fila de la tabla del frontend:
 * donde se eligió una disciplina y (opcional) una bonificación.
 */
public record FilaDisciplinaBonificacion(
        Long disciplinaId,
        Long bonificacionId
) {}