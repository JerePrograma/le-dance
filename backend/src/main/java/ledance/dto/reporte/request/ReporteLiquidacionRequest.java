// src/main/java/ledance/dto/reporte/request/ReporteLiquidacionRequest.java
package ledance.dto.reporte.request;

import ledance.dto.pago.response.DetallePagoResponse;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReporteLiquidacionRequest(
        @NotNull String fechaInicio,
        @NotNull String fechaFin,
        String disciplina,
        String profesor,
        @NotNull Double porcentaje,                    // nuevo campo
        @NotNull List<DetallePagoResponse> detalles
) {}
