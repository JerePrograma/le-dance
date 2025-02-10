package ledance.servicios;

import ledance.dto.request.BonificacionModificacionRequest;
import ledance.dto.request.BonificacionRegistroRequest;
import ledance.dto.response.BonificacionResponse;

import java.util.List;

public interface IBonificacionServicio {
    BonificacionResponse crearBonificacion(BonificacionRegistroRequest requestDTO);
    List<BonificacionResponse> listarBonificaciones();
    BonificacionResponse obtenerBonificacionPorId(Long id);
    BonificacionResponse actualizarBonificacion(Long id, BonificacionModificacionRequest requestDTO);
    void eliminarBonificacion(Long id);
}
