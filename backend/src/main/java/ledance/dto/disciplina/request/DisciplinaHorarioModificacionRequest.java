package ledance.dto.disciplina.request;

import ledance.entidades.DiaSemana;

import java.math.BigDecimal;
import java.time.LocalTime;

public record DisciplinaHorarioModificacionRequest(Long id, DiaSemana diaSemana, LocalTime horarioInicio, BigDecimal duracion) {
}
