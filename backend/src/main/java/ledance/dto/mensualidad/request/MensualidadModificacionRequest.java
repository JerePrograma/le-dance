package ledance.dto.mensualidad.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MensualidadModificacionRequest(
        @NotNull LocalDate fechaCuota,
        @NotNull @Min(0) Double valorBase,
        Long recargoId, // ✅ Se maneja como ID referenciado
        Long bonificacionId, // ✅ Se maneja como ID referenciado
        @NotNull String estado  // Se puede usar el valor del enum (ej. "PENDIENTE", "PAGADO", "OMITIDO")
) { }
