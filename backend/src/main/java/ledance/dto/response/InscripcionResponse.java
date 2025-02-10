package ledance.dto.response;

import ledance.entidades.EstadoInscripcion;

import java.time.LocalDate;

public record InscripcionResponse(
        Long id,
        AlumnoResumen alumno,
        DisciplinaResumen disciplina,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        String notas
) {
    public record AlumnoResumen(Long id, String nombre, String apellido) {}
    public record DisciplinaResumen(Long id, String nombre, String profesorNombre) {}
}