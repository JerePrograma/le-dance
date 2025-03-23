package ledance.dto.alumno.response;

import ledance.dto.inscripcion.response.InscripcionResponse;

import java.time.LocalDate;
import java.util.List;

public record AlumnoListadoResponse(
        Long id,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        LocalDate fechaIncorporacion,
        Integer edad,
        String celular1,
        String celular2,
        String email1,
        String email2,
        String documento,
        LocalDate fechaDeBaja,
        Boolean deudaPendiente,
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        Boolean activo,  // <-- Agregado aquí
        String otrasNotas,
        Double cuotaTotal
) {
}