package ledance.dto.caja;

import java.time.LocalDate;

public record CajaDiariaDTO(
        LocalDate fecha,
        String rangoRecibos,
        Double totalEfectivo,
        Double totalDebito,
        Double totalEgresos,
        Double totalNeto
) {}
