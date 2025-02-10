package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO para registrar un recargo con detalles dinámicos.
 */
public record RecargoRegistroRequest(
        @NotBlank String descripcion,
        @NotNull List<RecargoDetalleRegistroRequest> detalles // ✅ Nueva relación
) {}
