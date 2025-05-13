package ledance.dto.caja;

import java.time.LocalDate;

public record CajaPlanillaDTO(
        LocalDate fecha,
        String rangoRecibos,
        double totalEfectivo,
        double totalDebito,
        double totalEgresos,
        double totalNeto
) {}
