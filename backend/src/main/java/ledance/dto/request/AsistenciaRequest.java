package ledance.dto.request;

import java.time.LocalDate;

public record AsistenciaRequest(
        LocalDate fecha,
        Boolean presente,
        String observacion,
        Long alumnoId,
        Long disciplinaId,
        Long profesorId, // ✅ Nuevo campo opcional para el profesor
        Boolean activo  // ✅ Se agrega el campo activo
) {}
