package ledance.servicios.pago;

import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;

import java.util.List;

public interface IPagoServicio {
    PagoResponse registrarPago(PagoRegistroRequest request);

    PagoResponse obtenerPagoPorId(Long id);

    List<PagoResponse> listarPagos();

    PagoResponse actualizarPago(Long id, PagoModificacionRequest request);

    void eliminarPago(Long id);

    // ✅ Metodos adicionales
    List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId);

    List<PagoResponse> listarPagosPorAlumno(Long alumnoId);

    List<PagoResponse> listarPagosVencidos();
}