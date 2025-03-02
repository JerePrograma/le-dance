package ledance.servicios.mensualidad;

import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import java.util.List;

public interface IMensualidadService {
    MensualidadResponse crearMensualidad(MensualidadRegistroRequest request);
    MensualidadResponse actualizarMensualidad(Long id, MensualidadModificacionRequest request);
    MensualidadResponse obtenerMensualidad(Long id);
    List<MensualidadResponse> listarMensualidades();
    List<MensualidadResponse> listarPorInscripcion(Long inscripcionId);
    void eliminarMensualidad(Long id);
}
