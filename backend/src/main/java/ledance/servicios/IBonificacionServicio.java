package ledance.servicios;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;

import java.util.List;

public interface IBonificacionServicio {
    BonificacionResponse crearBonificacion(BonificacionRequest requestDTO);
    List<BonificacionResponse> listarBonificaciones();
    BonificacionResponse obtenerBonificacionPorId(Long id);
    BonificacionResponse actualizarBonificacion(Long id, BonificacionRequest requestDTO);
    void eliminarBonificacion(Long id);
}
