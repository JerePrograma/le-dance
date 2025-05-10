package ledance.dto.pago.response;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.usuario.response.UsuarioResponse;
import java.time.LocalDate;
import java.util.List;

public record PagoResponse(
        Long id,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Double monto,
        Double valorBase,
        Double importeInicial,
        Double montoPagado,
        Double saldoRestante,
        String estadoPago,
        AlumnoResponse alumno,
        MetodoPagoResponse metodoPago,
        String observaciones,
        List<DetallePagoResponse> detallePagos,
        Long usuarioId  // NUEVO: Cobrador
) { }
