package ledance.controladores;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.response.RolResponse;
import ledance.servicios.RolServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolControlador {

    private final RolServicio rolServicio;

    public RolControlador(RolServicio rolServicio) {
        this.rolServicio = rolServicio;
    }

    /**
     * Registra un nuevo rol.
     *
     * @param request Datos del rol a registrar.
     * @return El rol registrado.
     */
    @PostMapping
    public ResponseEntity<RolResponse> registrarRol(@RequestBody RolRegistroRequest request) {
        try {
            RolResponse response = rolServicio.registrarRol(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Obtiene un rol por su ID.
     *
     * @param id ID del rol.
     * @return Los datos del rol.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> obtenerRolPorId(@PathVariable Long id) {
        try {
            RolResponse response = rolServicio.obtenerRolPorId(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Lista todos los roles.
     *
     * @return Lista de roles.
     */
    @GetMapping
    public ResponseEntity<List<RolResponse>> listarRoles() {
        try {
            List<RolResponse> response = rolServicio.listarRoles();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
