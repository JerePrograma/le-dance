package ledance.dto.deudas;

import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.response.PagoResponse;
import java.util.List;

public record DeudasPendientesResponse(
        Long alumnoId,
        String alumnoNombre,
        List<PagoResponse> pagosPendientes,
        MatriculaResponse matriculaPendiente, // Puede ser null si está pagada o no existe
        List<MensualidadResponse> mensualidadesPendientes,
        Double totalDeuda
) { }
