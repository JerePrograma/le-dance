package ledance.dto.inscripcion.response;

import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.entidades.EstadoInscripcion;
import java.time.LocalDate;

public record InscripcionResponse(
        Long id,
        AlumnoListadoResponse alumno,
        DisciplinaListadoResponse disciplina, // Cambiado de disciplinaId a objeto
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        Double costoCalculado,
        BonificacionResponse bonificacion
) { }
