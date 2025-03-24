package ledance.dto.inscripcion.response;

import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.EstadoInscripcion;

import java.time.LocalDate;

public record InscripcionAlumnoResponse(
        Long id,
        DisciplinaResponse disciplina,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        Double costoCalculado,
        BonificacionResponse bonificacion,
        String mensualidadEstado
) {
}
