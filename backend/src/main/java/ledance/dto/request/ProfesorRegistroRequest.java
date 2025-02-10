package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ProfesorRegistroRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String especialidad,
        LocalDate fechaNacimiento, // ✅ NUEVO: Fecha de nacimiento opcional
        String telefono // ✅ NUEVO: Número de contacto opcional
) {}
