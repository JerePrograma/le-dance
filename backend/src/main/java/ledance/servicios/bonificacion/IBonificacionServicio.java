package ledance.servicios.bonificacion;

import ledance.dto.bonificacion.request.BonificacionModificacionRequest;
import ledance.dto.bonificacion.request.BonificacionRegistroRequest;
import ledance.dto.bonificacion.response.BonificacionResponse;

import java.util.List;

public interface IBonificacionServicio {
    BonificacionResponse crearBonificacion(BonificacionRegistroRequest requestDTO);

    List<BonificacionResponse> listarBonificaciones();

    BonificacionResponse obtenerBonificacionPorId(Long id);

    BonificacionResponse actualizarBonificacion(Long id, BonificacionModificacionRequest requestDTO);

    void eliminarBonificacion(Long id);
}