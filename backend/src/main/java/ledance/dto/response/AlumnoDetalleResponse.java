package ledance.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AlumnoDetalleResponse(
        Long id,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        Integer edad,
        String celular1,
        String celular2,
        String email1,
        String email2,
        String documento,
        String cuit,
        LocalDate fechaIncorporacion,
        LocalDate fechaDeBaja, // ✅ Nuevo campo
        Boolean deudaPendiente, // ✅ Nuevo campo
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        String otrasNotas,
        Double cuotaTotal,
        List<InscripcionResponse> inscripciones // ✅ Listado de inscripciones
) {}