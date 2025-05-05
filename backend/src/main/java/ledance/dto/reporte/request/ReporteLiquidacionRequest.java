// src/main/java/ledance/dto/reporte/request/ReporteLiquidacionRequest.java
package ledance.dto.reporte.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ledance.dto.pago.response.DetallePagoResponse;

import java.time.LocalDate;
import java.util.List;

public record ReporteLiquidacionRequest(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fechaInicio,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fechaFin,

        String disciplina,

        String profesor,

        @NotNull
        Double porcentaje,

        @NotEmpty
        List<DetallePagoResponse> detalles
) {}
