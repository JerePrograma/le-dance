package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.servicios.mensualidad.MensualidadServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensualidades")
@CrossOrigin(origins = "*") // Habilitar CORS si se accede desde frontend
public class MensualidadControlador {

    private final MensualidadServicio mensualidadServicio;

    public MensualidadControlador(MensualidadServicio mensualidadServicio) {
        this.mensualidadServicio = mensualidadServicio;
    }

    @PostMapping
    public ResponseEntity<MensualidadResponse> crearMensualidad(@Valid @RequestBody MensualidadRegistroRequest request) {
        MensualidadResponse response = mensualidadServicio.crearMensualidad(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MensualidadResponse> actualizarMensualidad(
            @PathVariable Long id,
            @Valid @RequestBody MensualidadModificacionRequest request) {
        MensualidadResponse response = mensualidadServicio.actualizarMensualidad(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MensualidadResponse> obtenerMensualidad(@PathVariable Long id) {
        MensualidadResponse response = mensualidadServicio.obtenerMensualidad(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MensualidadResponse>> listarMensualidades() {
        return ResponseEntity.ok(mensualidadServicio.listarMensualidades());
    }

    @GetMapping("/inscripcion/{inscripcionId}")
    public ResponseEntity<List<MensualidadResponse>> listarPorInscripcion(@PathVariable Long inscripcionId) {
        return ResponseEntity.ok(mensualidadServicio.listarPorInscripcion(inscripcionId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMensualidad(@PathVariable Long id) {
        mensualidadServicio.eliminarMensualidad(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para generar (o actualizar) las mensualidades de todas las inscripciones activas para el mes vigente.
     */
    @PostMapping("/generar-mensualidades")
    public ResponseEntity<List<MensualidadResponse>> generarMensualidadesParaMesVigente() {
        List<MensualidadResponse> respuestas = mensualidadServicio.generarMensualidadesParaMesVigente();
        return ResponseEntity.status(HttpStatus.CREATED).body(respuestas);
    }
}
