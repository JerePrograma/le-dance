// DisciplinaHorarioModificacionRequest.java
package ledance.dto.disciplina.request;

import ledance.entidades.DiaSemana;
import java.time.LocalTime;

public record DisciplinaHorarioModificacionRequest(
        Long id,           // Sera null para nuevos horarios
        DiaSemana diaSemana,
        LocalTime horarioInicio,
        Double duracion
) { }
