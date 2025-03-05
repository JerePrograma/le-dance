package ledance.servicios.rol;

import ledance.dto.rol.request.RolRegistroRequest;
import ledance.dto.rol.response.RolResponse;
import ledance.dto.rol.RolMapper;
import ledance.entidades.Rol;
import ledance.repositorios.RolRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolServicio implements IRolServicio {

    private static final Logger log = LoggerFactory.getLogger(RolServicio.class);

    private final RolRepositorio rolRepositorio;
    private final RolMapper rolMapper;

    public RolServicio(RolRepositorio rolRepositorio, RolMapper rolMapper) {
        this.rolRepositorio = rolRepositorio;
        this.rolMapper = rolMapper;
    }

    @Override
    @Transactional
    public RolResponse registrarRol(RolRegistroRequest request) {
        log.info("Registrando rol: {}", request.descripcion());
        if (rolRepositorio.existsByDescripcion(request.descripcion())) {
            throw new IllegalArgumentException("El rol ya existe.");
        }
        Rol rol = rolMapper.toEntity(request);
        rol.setDescripcion(rol.getDescripcion().toUpperCase());
        Rol guardado = rolRepositorio.save(rol);
        return rolMapper.toDTO(guardado);
    }

    @Override
    public RolResponse obtenerRolPorId(Long id) {
        Rol rol = rolRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
        return rolMapper.toDTO(rol);
    }

    @Override
    public List<RolResponse> listarRoles() {
        return rolRepositorio.findAll().stream()
                .map(rolMapper::toDTO)
                .collect(Collectors.toList());
    }
}