package ledance.dto.caja;

import java.time.LocalDate;
import java.util.List;

public record CajaPlanillaFlexDTO(
        LocalDate fecha,
        String rangoIds,
        List<MetodoTotalDTO> ingresosPorMetodo,
        double totalIngresos,
        List<MetodoTotalDTO> egresosPorMetodo,
        double totalEgresos,
        double neto
) {
}