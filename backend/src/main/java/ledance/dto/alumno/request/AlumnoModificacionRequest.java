package ledance.dto.alumno.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Peticion para modificar los datos de un alumno.
 * - "activo" se permite modificar para habilitar/deshabilitar el alumno.
 */
public record AlumnoModificacionRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotNull LocalDate fechaNacimiento,
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
        Boolean activo, // ✅ Ahora si se puede modificar
        List<InscripcionRegistroRequest> inscripciones // ✅ Puede modificarse la inscripcion
) {}
