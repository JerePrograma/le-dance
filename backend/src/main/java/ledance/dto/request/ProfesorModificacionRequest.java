package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProfesorModificacionRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        String especialidad,
        LocalDate fechaNacimiento,
        String telefono,
        @NotNull Boolean activo // ✅ Permite activar o desactivar el profesor
) {}
