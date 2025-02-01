package ledance.controladores;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.servicios.InscripcionServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionControlador {

    private final InscripcionServicio inscripcionServicio;

    public InscripcionControlador(InscripcionServicio inscripcionServicio) {
        this.inscripcionServicio = inscripcionServicio;
    }

    @PostMapping
    public ResponseEntity<InscripcionResponse> crear(@RequestBody InscripcionRequest request) {
        return ResponseEntity.ok(inscripcionServicio.crearInscripcion(request));
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long alumnoId) {
        try {
            if (alumnoId != null) {
                return ResponseEntity.ok(inscripcionServicio.listarPorAlumno(alumnoId));
            }
            return ResponseEntity.ok(inscripcionServicio.listarInscripciones());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscripcionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionServicio.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscripcionResponse> actualizar(@PathVariable Long id,
                                                          @RequestBody InscripcionRequest request) {
        return ResponseEntity.ok(inscripcionServicio.actualizarInscripcion(id, request));
    }

    @GetMapping("/detalles")
    public ResponseEntity<List<InscripcionResponse>> listarDetalles() {
        return ResponseEntity.ok(inscripcionServicio.listarInscripciones());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        inscripcionServicio.eliminarInscripcion(id);
        return ResponseEntity.ok("Inscripción eliminada exitosamente.");
    }
}
