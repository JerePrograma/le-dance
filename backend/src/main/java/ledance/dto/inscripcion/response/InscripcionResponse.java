package ledance.dto.inscripcion.response;

import ledance.entidades.EstadoInscripcion;
import java.time.LocalDate;

public record InscripcionResponse(
        Long id,
        Long alumnoId,
        Long disciplinaId,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        String notas,
        Double costoCalculado  // Nuevo campo
) { }
