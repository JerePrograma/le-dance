package ledance.servicios.usuario;

import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.dto.usuario.request.UsuarioModificacionRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.dto.usuario.UsuarioMapper;
import ledance.entidades.Usuario;
import ledance.entidades.Rol;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.PagoRepositorio;
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
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio,
                           PasswordEncoder passwordEncoder,
                           RolRepositorio rolRepositorio,
                           UsuarioMapper usuarioMapper, DetallePagoRepositorio detallePagoRepositorio, PagoRepositorio pagoRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.rolRepositorio = rolRepositorio;
        this.usuarioMapper = usuarioMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    @Override
    @Transactional
    public String registrarUsuario(UsuarioRegistroRequest datosRegistro) {
        log.info("Registrando usuario con nombre de usuario: {}", datosRegistro.nombreUsuario());
        if (usuarioRepositorio.findByNombreUsuario(datosRegistro.nombreUsuario()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
        }
        // Buscar el rol y lanzar error si no existe
        Rol rol = rolRepositorio.findByDescripcionIgnoreCase(datosRegistro.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + datosRegistro.rol()));
        Usuario usuario = usuarioMapper.toEntity(datosRegistro);
        usuario.setContrasena(passwordEncoder.encode(datosRegistro.contrasena()));
        usuario.setRol(rol);
        usuarioRepositorio.save(usuario);
        return "Usuario creado exitosamente.";
    }

    @Transactional
    public void editarUsuario(Long idUsuario, UsuarioModificacionRequest modificacionRequest) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        // Actualizar nombre si se envia y no es vacio
        if (modificacionRequest.nombreUsuario() != null && !modificacionRequest.nombreUsuario().isBlank()) {
            usuario.setNombreUsuario(modificacionRequest.nombreUsuario());
        }
        // Actualizar contraseña si se envia y no es vacio (se codifica)
        if (modificacionRequest.contrasena() != null && !modificacionRequest.contrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(modificacionRequest.contrasena()));
        }
        // Actualizar rol si se envia y no es vacio
        if (modificacionRequest.rol() != null && !modificacionRequest.rol().isBlank()) {
            Rol rol = rolRepositorio.findByDescripcion(modificacionRequest.rol().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + modificacionRequest.rol()));
            usuario.setRol(rol);
        }
        // Actualizar el estado activo (alta o baja)
        if (modificacionRequest.activo() != null) {
            usuario.setActivo(modificacionRequest.activo());
        }
        usuarioRepositorio.save(usuario);
    }

    public UsuarioResponse obtenerUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return convertirAUsuarioResponse(usuario);
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

    @Transactional
    public void eliminarUsuario(Long idUsuario) {
        // 1) Poner a null las FKs en DetallePago y Pago
        detallePagoRepositorio.clearUsuarioFromDetallePagos(idUsuario);
        pagoRepositorio.clearUsuarioFromPagos(idUsuario);

        // 2) Ahora sí, eliminar el usuario sin conflictos
        usuarioRepositorio.deleteById(idUsuario);
    }
}
