package ledance.servicios;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.response.RolResponse;

import java.util.List;

public interface IRolServicio {
    RolResponse registrarRol(RolRegistroRequest request);
    RolResponse obtenerRolPorId(Long id);
    List<RolResponse> listarRoles();
}
