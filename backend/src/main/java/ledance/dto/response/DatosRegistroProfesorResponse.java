package ledance.dto.response;

import ledance.entidades.Profesor;

public record DatosRegistroProfesorResponse(
        Long id,
        String nombre,
        String apellido,
        String especialidad,
        Integer aniosExperiencia
) {
    public DatosRegistroProfesorResponse(Profesor profesor) {
        this(profesor.getId(), profesor.getNombre(), profesor.getApellido(),
                profesor.getEspecialidad(), profesor.getAniosExperiencia());
    }
}
