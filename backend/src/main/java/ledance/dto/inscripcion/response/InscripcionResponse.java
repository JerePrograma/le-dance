package ledance.dto.inscripcion.response;

import ledance.entidades.EstadoInscripcion;

import java.time.LocalDate;

// Ejemplo de InscripcionResponse actualizado
public record InscripcionResponse(
        Long id,
        AlumnoResumen alumno,
        DisciplinaResumen disciplina,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado,
        String notas,
        Double costoCalculado  // Nuevo campo
) {
    public record AlumnoResumen(Long id, String nombre, String apellido) {
    }

    public record DisciplinaResumen(Long id, String nombre, String profesorNombre) {
    }
}