package ledance.dto.reporte.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para registrar un nuevo reporte.
 */
public record ReporteRegistroRequest(
        @NotBlank String tipo, // ✅ Tipo de reporte (Recaudacion, Asistencia, Pagos)
        @NotBlank String descripcion, // ✅ Descripcion del reporte
        @NotNull LocalDate fechaGeneracion, // ✅ Fecha de generacion del reporte
        Long usuarioId // ✅ Opcional: Puede ser nulo si no se asigna usuario al crear el reporte
) {}
