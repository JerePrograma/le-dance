package ledance.dto.recargo.request;

import jakarta.validation.constraints.*;

public record RecargoRegistroRequest(
        @NotBlank String descripcion,
        @NotNull Double porcentaje,
        Double valorFijo,
        @NotNull @Min(1) @Max(31) Integer diaDelMesAplicacion // ✅ Día específico del mes
) {}
