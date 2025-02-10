package ledance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Petición para registrar un nuevo movimiento en la caja.
 * - "activo" se asigna automáticamente en el servicio.
 */
public record CajaRegistroRequest(
        @NotNull LocalDate fecha,
        @NotNull @PositiveOrZero Double totalEfectivo,
        @NotNull @PositiveOrZero Double totalTransferencia,
        @NotNull @PositiveOrZero Double totalTarjeta, // ✅ Nuevo campo agregado
        String rangoDesdeHasta,
        String observaciones
) {}
