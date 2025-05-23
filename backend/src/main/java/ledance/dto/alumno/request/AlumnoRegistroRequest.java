package ledance.dto.alumno.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Peticion para registrar un alumno.
 * - "edad" no se recibe, se calcula en el backend.
 * - "disciplinas" son las disciplinas en las que se inscribe con sus bonificaciones.
 * - "activo" no se recibe, se establece automaticamente como `true`.
 */
public record AlumnoRegistroRequest(
        Long id,
        @NotBlank String nombre,
        @NotBlank String apellido,
        LocalDate fechaNacimiento,
        LocalDate fechaIncorporacion,
        Integer edad,
        String celular1,
        String celular2,
        @Email String email,
        String documento,
        LocalDate fechaDeBaja,
        Boolean deudaPendiente,
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        Boolean activo,  // <-- Agregado aqui
        String otrasNotas,
        Double cuotaTotal,
        List<InscripcionRegistroRequest> inscripciones // ✅ Relacion con disciplinas y bonificaciones
) {}
