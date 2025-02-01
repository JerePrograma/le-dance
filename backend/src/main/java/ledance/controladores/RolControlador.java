package ledance.controladores;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.response.RolResponse;
import ledance.servicios.RolServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Validated
public class RolControlador {

    private final RolServicio rolService;

    public RolControlador(RolServicio rolService) {
        this.rolService = rolService;
    }

    @PostMapping
    public ResponseEntity<RolResponse> registrarRol(@RequestBody @Validated RolRegistroRequest request) {
        RolResponse response = rolService.registrarRol(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> obtenerRolPorId(@PathVariable Long id) {
        RolResponse response = rolService.obtenerRolPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RolResponse>> listarRoles() {
        List<RolResponse> respuesta = rolService.listarRoles();
        return ResponseEntity.ok(respuesta);
    }
}
