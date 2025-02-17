package ledance.servicios.usuario;

import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.dto.usuario.UsuarioMapper;
import ledance.entidades.Usuario;
import ledance.entidades.Rol;
import ledance.repositorios.UsuarioRepositorio;
import ledance.repositorios.RolRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServicio implements IUsuarioServicio {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServicio.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final RolRepositorio rolRepositorio;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio,
                           PasswordEncoder passwordEncoder,
                           RolRepositorio rolRepositorio,
                           UsuarioMapper usuarioMapper) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.rolRepositorio = rolRepositorio;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional
    public String registrarUsuario(UsuarioRegistroRequest datosRegistro) {
        log.info("Registrando usuario con nombre de usuario: {}", datosRegistro.nombreUsuario());
        if (usuarioRepositorio.findByNombreUsuario(datosRegistro.nombreUsuario()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
        }
        Rol rol = rolRepositorio.findByDescripcion(datosRegistro.rol().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + datosRegistro.rol()));
        Usuario usuario = usuarioMapper.toEntity(datosRegistro);
        usuario.setContrasena(passwordEncoder.encode(datosRegistro.contrasena()));
        usuario.setRol(rol);
        usuarioRepositorio.save(usuario);
        return "Usuario creado exitosamente.";
    }

    @Override
    @Transactional
    public void actualizarNombreDeUsuario(Long idUsuario, String nuevoNombre) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setNombreUsuario(nuevoNombre);
        usuarioRepositorio.save(usuario);
    }

    @Override
    @Transactional
    public void actualizarRol(Long idUsuario, Rol nuevoRol) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setRol(nuevoRol);
        usuarioRepositorio.save(usuario);
    }

    @Override
    @Transactional
    public void actualizarRolPorDescripcion(Long idUsuario, String descripcionRol) {
        Rol rol = rolRepositorio.findByDescripcion(descripcionRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + descripcionRol));
        actualizarRol(idUsuario, rol);
    }

    @Override
    @Transactional
    public void desactivarUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepositorio.save(usuario);
    }

    @Override
    public List<Usuario> listarUsuarios(String rolDescripcion, Boolean activo) {
        if (rolDescripcion != null && activo != null) {
            Rol rol = rolRepositorio.findByDescripcion(rolDescripcion)
                    .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + rolDescripcion));
            return usuarioRepositorio.findByRolAndActivo(rol, activo);
        } else if (rolDescripcion != null) {
            Rol rol = rolRepositorio.findByDescripcion(rolDescripcion)
                    .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + rolDescripcion));
            return usuarioRepositorio.findByRol(rol);
        } else if (activo != null) {
            return usuarioRepositorio.findByActivo(activo);
        } else {
            return usuarioRepositorio.findAll();
        }
    }

    @Override
    public UsuarioResponse convertirAUsuarioResponse(Usuario usuario) {
        return usuarioMapper.toDTO(usuario);
    }
}