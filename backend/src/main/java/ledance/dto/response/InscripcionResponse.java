package ledance.dto.response;

public record InscripcionResponse(
        Long id,
        Long alumnoId,
        Long disciplinaId,
        Long bonificacionId,
        Double costoParticular,
        String notas
) {}
