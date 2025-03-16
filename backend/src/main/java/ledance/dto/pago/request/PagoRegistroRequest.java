package ledance.dto.pago.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;

import java.time.LocalDate;
import java.util.List;

public record PagoRegistroRequest(
        Long id,
        @NotNull LocalDate fecha,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Min(0) Double monto,
        InscripcionRegistroRequest inscripcion,
        Long metodoPagoId,   // Opcional
        Boolean recargoAplicado,
        Boolean bonificacionAplicada,
        Boolean pagoMatricula,
        AlumnoRegistroRequest alumno,
        @NotNull List<DetallePagoRegistroRequest> detallePagos,
        List<PagoMedioRegistroRequest> pagoMedios,
        boolean activo
) { }
