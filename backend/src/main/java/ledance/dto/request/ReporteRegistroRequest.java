package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para registrar un nuevo reporte.
 */
public record ReporteRegistroRequest(
        @NotBlank String tipo, // ✅ Tipo de reporte (Recaudación, Asistencia, Pagos)
        @NotBlank String descripcion, // ✅ Descripción del reporte
        @NotNull LocalDate fechaGeneracion, // ✅ Fecha de generación del reporte
        Long usuarioId // ✅ Opcional: Puede ser nulo si no se asigna usuario al crear el reporte
) {}
