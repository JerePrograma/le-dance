package ledance.dto.pago.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ledance.dto.alumno.request.AlumnoRegistroRequest;

import java.time.LocalDate;
import java.util.List;

public record PagoRegistroRequest(
        Long id,
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        @NotNull @Min(0) Double importeInicial,
        Double valorBase,
        Long metodoPagoId,
        @NotNull AlumnoRegistroRequest alumno,
        String observaciones,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        Boolean activo,
        Long usuarioId,
        String estadoPago,
        @JsonProperty("aplicarRecargoMetodoPago")
        Boolean recargoMetodoPagoAplicado
) {
}
