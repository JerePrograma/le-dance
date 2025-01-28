package ledance.dto.response;

import java.time.LocalDate;

public record AsistenciaResponseDTO(
        Long id,
        LocalDate fecha,
        Boolean presente,
        String observacion,
        Long alumnoId,
        Long disciplinaId
) {}
