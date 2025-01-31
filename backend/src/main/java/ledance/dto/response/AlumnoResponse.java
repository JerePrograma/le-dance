package ledance.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AlumnoResponse(
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
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        Boolean activo,
        List<DisciplinaSimpleResponse> disciplinas
) {}



