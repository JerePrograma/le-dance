package ledance.controladores;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.servicios.BonificacionServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bonificaciones")
@Validated
public class BonificacionControlador {

    private static final Logger log = LoggerFactory.getLogger(BonificacionControlador.class);
    private final BonificacionServicio bonificacionService;

    public BonificacionControlador(BonificacionServicio bonificacionService) {
        this.bonificacionService = bonificacionService;
    }

    @PostMapping
    public ResponseEntity<BonificacionResponse> crearBonificacion(@RequestBody @Validated BonificacionRequest requestDTO) {
        log.info("Creando bonificacion: {}", requestDTO.descripcion());
        BonificacionResponse response = bonificacionService.crearBonificacion(requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BonificacionResponse>> listarBonificaciones() {
        List<BonificacionResponse> respuesta = bonificacionService.listarBonificaciones();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonificacionResponse> obtenerBonificacionPorId(@PathVariable Long id) {
        BonificacionResponse response = bonificacionService.obtenerBonificacionPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BonificacionResponse> actualizarBonificacion(@PathVariable Long id,
                                                                       @RequestBody @Validated BonificacionRequest requestDTO) {
        BonificacionResponse response = bonificacionService.actualizarBonificacion(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarBonificacion(@PathVariable Long id) {
        bonificacionService.eliminarBonificacion(id);
        return ResponseEntity.ok("Bonificacion eliminada exitosamente.");
    }
}
