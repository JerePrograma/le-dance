package ledance.dto.matricula.request;

import java.time.LocalDate;

public record MatriculaRegistroRequest(
        Long alumnoId,
        Integer anio,
        Boolean pagada,
        LocalDate fechaPago
) {
}