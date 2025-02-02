package ledance.controladores;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.servicios.InscripcionServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
@Validated
public class InscripcionControlador {

    private static final Logger log = LoggerFactory.getLogger(InscripcionControlador.class);
    private final InscripcionServicio inscripcionService;

    public InscripcionControlador(InscripcionServicio inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @PostMapping
    public ResponseEntity<InscripcionResponse> crear(@RequestBody @Validated InscripcionRequest request) {
        log.info("Creando inscripcion para alumnoId: {} en disciplinaId: {}", request.alumnoId(), request.disciplinaId());
        InscripcionResponse response = inscripcionService.crearInscripcion(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long alumnoId) {
        try {
            if (alumnoId != null) {
                List<InscripcionResponse> respuesta = inscripcionService.listarPorAlumno(alumnoId);
                return ResponseEntity.ok(respuesta);
            }
            return ResponseEntity.ok(inscripcionService.listarInscripciones());
        } catch (IllegalArgumentException e) {
            log.error("Error listando inscripciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error interno listando inscripciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscripcionResponse> obtenerPorId(@PathVariable Long id) {
        InscripcionResponse response = inscripcionService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscripcionResponse> actualizar(@PathVariable Long id,
                                                          @RequestBody @Validated InscripcionRequest request) {
        InscripcionResponse response = inscripcionService.actualizarInscripcion(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detalles")
    public ResponseEntity<List<InscripcionResponse>> listarDetalles() {
        List<InscripcionResponse> respuesta = inscripcionService.listarInscripciones();
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        inscripcionService.eliminarInscripcion(id);
        return ResponseEntity.ok("Inscripcion eliminada exitosamente.");
    }
}
