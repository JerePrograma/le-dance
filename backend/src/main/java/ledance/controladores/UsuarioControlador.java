package ledance.controladores;

import ledance.entidades.Rol;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ledance.dto.request.*;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.servicios.UsuarioServicio;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControlador {

    private final UsuarioServicio usuarioServicio;

    public UsuarioControlador(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    // Registro de usuario
    @PostMapping("/registro")
    public ResponseEntity<String> registrarUsuario(@RequestBody UsuarioRegistroRequest datosRegistro) {
        try {
            String mensaje = usuarioServicio.registrarUsuario(datosRegistro);
            return ResponseEntity.ok(mensaje);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    // Obtener perfil del usuario autenticado
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(usuarioServicio.convertirAUsuarioResponse(usuario));
    }

    // Actualizar nombre de usuario
    @PatchMapping("/perfil")
    public ResponseEntity<String> actualizarNombreUsuario(@AuthenticationPrincipal Usuario usuario,
                                                          @RequestBody ModificacionUsuarioRequest datos) {
        if (usuario == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado");
        }
        try {
            usuarioServicio.actualizarNombreDeUsuario(usuario.getId(), datos.nombreUsuario());
            return ResponseEntity.ok("Nombre de usuario actualizado a: " + datos.nombreUsuario());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<String> actualizarRol(@PathVariable Long id, @RequestParam String nuevoRol) {
        try {
            usuarioServicio.actualizarRolPorDescripcion(id, nuevoRol.toUpperCase());
            return ResponseEntity.ok("Rol actualizado a: " + nuevoRol);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    // Desactivar usuario
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<String> desactivarUsuario(@PathVariable Long id) {
        try {
            usuarioServicio.desactivarUsuario(id);
            return ResponseEntity.ok("Usuario desactivado.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    // Listar usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(@RequestParam(required = false) String rol,
                                                                @RequestParam(required = false) Boolean activo) {
        try {
            List<UsuarioResponse> usuarios = usuarioServicio.listarUsuarios(rol, activo)
                    .stream()
                    .map(usuarioServicio::convertirAUsuarioResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
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
