package ledance.servicios;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.response.RolResponse;
import ledance.entidades.Rol;
import ledance.repositorios.RolRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolServicio {

    private final RolRepositorio rolRepositorio;

    public RolServicio(RolRepositorio rolRepositorio) {
        this.rolRepositorio = rolRepositorio;
    }

    /**
     * Registra un nuevo rol.
     *
     * @param request Datos del rol.
     * @return Rol registrado.
     */
    public RolResponse registrarRol(RolRegistroRequest request) {
        if (rolRepositorio.existsByDescripcion(request.descripcion())) {
            throw new IllegalArgumentException("El rol ya existe.");
        }

        Rol rol = new Rol();
        rol.setDescripcion(request.descripcion());
        rol = rolRepositorio.save(rol);

        return new RolResponse(rol.getId(), rol.getDescripcion());
    }

    /**
     * Obtiene un rol por su ID.
     *
     * @param id ID del rol.
     * @return Datos del rol.
     */
    public RolResponse obtenerRolPorId(Long id) {
        Rol rol = rolRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
        return new RolResponse(rol.getId(), rol.getDescripcion());
    }

    /**
     * Lista todos los roles.
     *
     * @return Lista de roles.
     */
    public List<RolResponse> listarRoles() {
        return rolRepositorio.findAll()
                .stream()
                .map(rol -> new RolResponse(rol.getId(), rol.getDescripcion()))
                .collect(Collectors.toList());
    }
}
