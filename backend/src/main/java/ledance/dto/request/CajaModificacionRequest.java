package ledance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Petición para modificar un movimiento de caja existente.
 * - Permite cambiar "activo" (activar o desactivar el movimiento).
 */
public record CajaModificacionRequest(
        @NotNull LocalDate fecha,
        @NotNull @PositiveOrZero Double totalEfectivo,
        @NotNull @PositiveOrZero Double totalTransferencia,
        @NotNull @PositiveOrZero Double totalTarjeta, // ✅ Nuevo campo agregado
        String rangoDesdeHasta,
        String observaciones,
        Boolean activo // ✅ Ahora se puede modificar el estado
) {}
