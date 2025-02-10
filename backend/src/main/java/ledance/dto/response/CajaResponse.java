package ledance.dto.response;

import java.time.LocalDate;

public record CajaResponse(
        Long id,
        LocalDate fecha,
        Double totalEfectivo,
        Double totalTransferencia,
        Double totalTarjeta, // ✅ Nuevo campo
        String rangoDesdeHasta,
        String observaciones,
        Boolean activo
) {}
