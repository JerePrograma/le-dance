package ledance.controladores;

import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.dto.usuario.request.UsuarioModificacionRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.servicios.usuario.UsuarioServicio;
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

    @PutMapping("/{id}")
    public ResponseEntity<String> editarUsuario(@PathVariable Long id,
                                                @RequestBody @Validated UsuarioModificacionRequest modificacionRequest) {
        usuarioService.editarUsuario(id, modificacionRequest);
        return ResponseEntity.ok("Usuario actualizado correctamente.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.ok("Usuario eliminado.");
    }

}
