package ledance.dto.request;


/**
 * Representa UNA fila de la tabla del frontend:
 * donde se eligio una disciplina y (opcional) una bonificacion.
 */
public record FilaDisciplinaBonificacion(
        Long disciplinaId,
        Long bonificacionId
) {}