package ledance.dto.alumno.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        @NotNull LocalDate fechaIncorporacion,
        String celular1,
        String celular2,
        @Email String email1,
        @Email String email2,
        String documento,
        String cuit,
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        String otrasNotas,
        Double cuotaTotal,
        List<InscripcionRegistroRequest> inscripciones // ✅ Relacion con disciplinas y bonificaciones
) {}
