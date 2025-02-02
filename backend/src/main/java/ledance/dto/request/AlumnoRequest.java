package ledance.dto.request;

import java.time.LocalDate;
import java.util.List;

/**
 * Peticion para crear/actualizar un Alumno, analoga a tu Frontend.
 * - "edad" no se recibe, la calculamos en el backend.
 * - "filas" es la lista de la tabla (Disciplina+Bonificacion).
 */
public record AlumnoRequest(
        String nombre,
        String apellido, // ✅ Agregar apellido si no existe
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
        Double cuotaTotal,
        List<FilaDisciplinaBonificacion> filas
) {}

