package ledance.dto.response;

import ledance.entidades.EstadoAsistencia;

import java.time.LocalDate;

public record AsistenciaDiariaResponse(
        Long id,
        LocalDate fecha,
        EstadoAsistencia estado, // ✅ Ahora es seguro
        Long alumnoId,
        String alumnoNombre,
        String alumnoApellido,
        Long asistenciaMensualId,
        String observacion, // ✅ Mantener como opcional
        Long disciplinaId // ✅ Agregado para mejor contexto
) {}
