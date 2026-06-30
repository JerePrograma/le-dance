package ledance.dto.alumno.response;

import java.time.LocalDate;

public record AlumnoListadoResponse(
        Long id,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        LocalDate fechaIncorporacion,
        Integer edad,
        String celular1,
        String celular2,
        String email,
        String documento,
        LocalDate fechaDeBaja,
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        Boolean activo,
        String otrasNotas
) {
}
