package ledance.dto.inscripcion.response;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.EstadoInscripcion;
import java.time.LocalDate;

public record InscripcionResponse(
        Long id,
        AlumnoResponse alumno,
        DisciplinaResponse disciplina,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        Double costoCalculado,
        BonificacionResponse bonificacion,
        String mensualidadEstado
) { }
