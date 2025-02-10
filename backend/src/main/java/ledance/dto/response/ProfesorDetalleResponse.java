package ledance.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ProfesorDetalleResponse(
        Long id,
        String nombre,
        String apellido,
        String especialidad,
        LocalDate fechaNacimiento, // ✅ NUEVO
        Integer edad, // ✅ Se calcula automáticamente
        String telefono, // ✅ NUEVO
        Boolean activo,
        List<DisciplinaListadoResponse> disciplinas // ✅ Lista de disciplinas que imparte
) {}
