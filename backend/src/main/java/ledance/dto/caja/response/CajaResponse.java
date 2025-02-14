package ledance.dto.caja.response;

import java.time.LocalDate;

public record CajaResponse(
        Long id,
        LocalDate fecha,
        Double totalEfectivo,
        Double totalTransferencia,
        Double totalTarjeta,
        String rangoDesdeHasta,
        String observaciones,
        Boolean activo
) {}
