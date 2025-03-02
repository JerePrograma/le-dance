package ledance.dto.caja;

import java.time.LocalDate;

// En tu package ledance.dto.caja
public record CajaDiariaDTO(
        LocalDate fecha,
        String rangoRecibos,
        Double totalEfectivo,
        Double totalDebito,
        Double totalEgresos,
        Double totalNeto
) {}
