package ledance.servicios.rol;

import ledance.dto.rol.request.RolRegistroRequest;
import ledance.dto.rol.response.RolResponse;

import java.util.List;

public interface IRolServicio {
    RolResponse registrarRol(RolRegistroRequest request);

    RolResponse obtenerRolPorId(Long id);

    List<RolResponse> listarRoles();
}