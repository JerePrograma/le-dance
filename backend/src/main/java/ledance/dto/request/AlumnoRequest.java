package ledance.dto.request;

import java.time.LocalDate;
import java.util.List;

/**
 * Petición para crear/actualizar un Alumno, análoga a tu Frontend.
 * - "edad" no se recibe, la calculamos en el backend.
 * - "filas" es la lista de la tabla (Disciplina+Bonificación).
 */
public record AlumnoRequest(
        String nombre,
        LocalDate fechaNacimiento,
        LocalDate fechaIncorporacion,
        String celular1,
        String celular2,
        String telefono,
        String email1,
        String email2,
        String documento,
        String cuit,
        String nombrePadres,
        Boolean autorizadoParaSalirSolo,
        Boolean activo,
        String otrasNotas,
        Double cuotaTotal, // El front la calcula y la envía, o la calculamos en backend
        List<FilaDisciplinaBonificacion> filas
) {}
