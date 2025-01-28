package ledance.servicios;

import ledance.entidades.Rol;
import ledance.repositorios.RolRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ledance.dto.request.*;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.repositorios.UsuarioRepositorio;

import java.util.List;

@Service
public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final RolRepositorio rolRepositorio;

    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder, RolRepositorio rolRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.rolRepositorio = rolRepositorio;
    }

    public String registrarUsuario(UsuarioRegistroRequest datosRegistro) {
        if (usuarioRepositorio.findByEmail(datosRegistro.email()).isPresent()) {
            throw new IllegalArgumentException("El email ya esta en uso.");
        }

        Rol rol = rolRepositorio.findByDescripcion(datosRegistro.rol().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + datosRegistro.rol()));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombreUsuario(datosRegistro.nombreUsuario());
        nuevoUsuario.setEmail(datosRegistro.email());
        nuevoUsuario.setContrasena(passwordEncoder.encode(datosRegistro.contrasena()));
        nuevoUsuario.setRol(rol);
        usuarioRepositorio.save(nuevoUsuario);

        return "Usuario creado exitosamente.";
    }

    public void actualizarNombreDeUsuario(Long idUsuario, String nuevoNombre) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setNombreUsuario(nuevoNombre);
        usuarioRepositorio.save(usuario);
    }

    public void actualizarRol(Long idUsuario, Rol nuevoRol) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setRol(nuevoRol);
        usuarioRepositorio.save(usuario);
    }

    public void actualizarRolPorDescripcion(Long idUsuario, String descripcionRol) {
        Rol rol = rolRepositorio.findByDescripcion(descripcionRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + descripcionRol));
        actualizarRol(idUsuario, rol);
    }

    public void desactivarUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepositorio.save(usuario);
    }

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

    public UsuarioResponse convertirAUsuarioResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getRol().getDescripcion() // Obtener descripcion del rol
        );
    }
}
