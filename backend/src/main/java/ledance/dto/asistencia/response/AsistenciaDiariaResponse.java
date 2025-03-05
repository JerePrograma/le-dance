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
        Long asistenciaMensualId,
        Long disciplinaId // Ahora obtenido directamente desde asistenciaMensual.disciplina.id
) {}
