package ledance.controladores;

import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.servicios.egreso.EgresoServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/egresos")
@Validated
public class EgresoControlador {

    private final EgresoServicio egresoServicio;

    public EgresoControlador(EgresoServicio egresoServicio) {
        this.egresoServicio = egresoServicio;
    }

    // Registrar un nuevo egreso (usando DTO en el body)
    @PostMapping
    public ResponseEntity<EgresoResponse> registrarEgreso(@RequestBody EgresoRegistroRequest request) {
        EgresoResponse response = egresoServicio.agregarEgreso(request);
        return ResponseEntity.ok(response);
    }

    // Actualizar un egreso existente
    @PutMapping("/{id}")
    public ResponseEntity<EgresoResponse> actualizarEgreso(@PathVariable Long id,
                                                           @RequestBody EgresoRegistroRequest request) {
        EgresoResponse response = egresoServicio.actualizarEgreso(id, request);
        return ResponseEntity.ok(response);
    }

    // Eliminar (Baja logica) un egreso
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEgreso(@PathVariable Long id) {
        egresoServicio.eliminarEgreso(id);
        return ResponseEntity.noContent().build();
    }

    // Obtener un egreso por ID
    @GetMapping("/{id}")
    public ResponseEntity<EgresoResponse> obtenerEgreso(@PathVariable Long id) {
        EgresoResponse response = egresoServicio.obtenerEgresoPorId(id);
        return ResponseEntity.ok(response);
    }

    // Listar todos los egresos activos
    @GetMapping
    public ResponseEntity<List<EgresoResponse>> listarEgresos() {
        List<EgresoResponse> response = egresoServicio.listarEgresos();
        return response.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    // Listar egresos de tipo DEBITO
    @GetMapping("/debito")
    public ResponseEntity<List<EgresoResponse>> listarEgresosDebito() {
        List<EgresoResponse> response = egresoServicio.listarEgresosDebito();
        return response.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    // Listar egresos de tipo EFECTIVO
    @GetMapping("/efectivo")
    public ResponseEntity<List<EgresoResponse>> listarEgresosEfectivo() {
        List<EgresoResponse> response = egresoServicio.listarEgresosEfectivo();
        return response.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

}
