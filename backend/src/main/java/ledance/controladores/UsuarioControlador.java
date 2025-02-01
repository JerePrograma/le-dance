package ledance.controladores;

import ledance.dto.request.ModificacionUsuarioRequest;
import ledance.dto.request.UsuarioRegistroRequest;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.servicios.UsuarioServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@Validated
public class UsuarioControlador {

    private final UsuarioServicio usuarioService;

    public UsuarioControlador(UsuarioServicio usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<String> registrarUsuario(@RequestBody @Validated UsuarioRegistroRequest datosRegistro) {
        String mensaje = usuarioService.registrarUsuario(datosRegistro);
        return ResponseEntity.ok(mensaje);
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(usuarioService.convertirAUsuarioResponse(usuario));
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> actualizarNombreUsuario(@AuthenticationPrincipal Usuario usuario,
                                                          @RequestBody @Validated ModificacionUsuarioRequest datos) {
        if (usuario == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado");
        }
        usuarioService.actualizarNombreDeUsuario(usuario.getId(), datos.nombreUsuario());
        return ResponseEntity.ok("Nombre de usuario actualizado a: " + datos.nombreUsuario());
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<String> actualizarRol(@PathVariable Long id, @RequestParam String nuevoRol) {
        usuarioService.actualizarRolPorDescripcion(id, nuevoRol.toUpperCase());
        return ResponseEntity.ok("Rol actualizado a: " + nuevoRol);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<String> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok("Usuario desactivado.");
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(@RequestParam(required = false) String rol,
                                                                @RequestParam(required = false) Boolean activo) {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios(rol, activo)
                .stream()
                .map(usuarioService::convertirAUsuarioResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }
}
