package ledance.controladores;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.servicios.BonificacionServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bonificaciones")
public class BonificacionControlador {

    private final BonificacionServicio bonificacionServicio;

    public BonificacionControlador(BonificacionServicio bonificacionServicio) {
        this.bonificacionServicio = bonificacionServicio;
    }

    @PostMapping
    public ResponseEntity<BonificacionResponse> crearBonificacion(@RequestBody BonificacionRequest requestDTO) {
        return ResponseEntity.ok(bonificacionServicio.crearBonificacion(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<BonificacionResponse>> listarBonificaciones() {
        return ResponseEntity.ok(bonificacionServicio.listarBonificaciones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonificacionResponse> obtenerBonificacionPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bonificacionServicio.obtenerBonificacionPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BonificacionResponse> actualizarBonificacion(@PathVariable Long id,
                                                                       @RequestBody BonificacionRequest requestDTO) {
        return ResponseEntity.ok(bonificacionServicio.actualizarBonificacion(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarBonificacion(@PathVariable Long id) {
        bonificacionServicio.eliminarBonificacion(id);
        return ResponseEntity.ok("Bonificacion eliminada exitosamente.");
    }
}
