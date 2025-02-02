package ledance.dto.response;

import ledance.entidades.Profesor;

public record DatosRegistroProfesorResponse(
        Long id,
        String nombre,
        String apellido,
        String especialidad,
        Boolean activo // ✅ Agregado para evitar error
) {}

