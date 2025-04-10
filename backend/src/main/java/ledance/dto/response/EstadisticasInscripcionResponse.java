package ledance.dto.response;

import java.util.Map;

public record EstadisticasInscripcionResponse(
        long totalInscripciones,
        Map<String, Long> inscripcionesPorDisciplina,
        Map<Integer, Long> inscripcionesPorMes
) {
}