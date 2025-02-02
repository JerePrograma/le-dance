package ledance.dto.response;

import java.util.List;

public record ProfesorResponse(
        Long id,
        String nombre,
        String apellido,
        String especialidad,
        Boolean activo,
        List<DisciplinaResponse> disciplinas // ✅ Agregado para corregir el error
) {}
