package ledance.dto.recargodetalle.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para registrar detalles de un recargo (día desde y porcentaje).
 */
public record RecargoDetalleRegistroRequest(
        @NotNull Integer diaDesde,
        @NotNull Double porcentaje
) {}
