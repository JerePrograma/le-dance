package ledance.dto.caja.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CajaModificacionRequest(
        @NotNull LocalDate fecha,
        @NotNull @PositiveOrZero Double totalEfectivo,
        @NotNull @PositiveOrZero Double totalTransferencia,
        @NotNull @PositiveOrZero Double totalTarjeta,
        String rangoDesdeHasta,
        String observaciones,
        Boolean activo
) {}
