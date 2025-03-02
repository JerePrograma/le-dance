package ledance.dto.asistencia.response;

import ledance.entidades.EstadoAsistencia;
import java.time.LocalDate;

public record AsistenciaDiariaResponse(
        Long id,
        LocalDate fecha,
        EstadoAsistencia estado,
        Long alumnoId,
        String alumnoNombre,
        String alumnoApellido,
        Long disciplinaHorarioId, // ✅ Ahora referencia al horario en lugar de AsistenciaMensual
        String horarioInicio, // ✅ Se incluye la hora de inicio de la clase
        String observacion
) {}
