package ledance.dto.request;

import java.time.LocalDate;

public record AsistenciaRequest(
        LocalDate fecha,
        Boolean presente,
        String observacion,
        Long alumnoId,
        Long disciplinaId
) {}
