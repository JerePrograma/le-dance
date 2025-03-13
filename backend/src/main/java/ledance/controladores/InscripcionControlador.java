package ledance.controladores;

import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.response.EstadisticasInscripcionResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.servicios.inscripcion.InscripcionServicio;
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
    private final InscripcionServicio inscripcionServicio;

    public InscripcionControlador(InscripcionServicio inscripcionServicio) {
        this.inscripcionServicio = inscripcionServicio;
    }

    @PostMapping
    public ResponseEntity<InscripcionResponse> crear(@RequestBody @Validated InscripcionRegistroRequest request) {
        log.info("Creando inscripción para alumnoId: {} en disciplinaId: {}",
                request.alumnoId(), request.disciplina().id());
        InscripcionResponse response = inscripcionServicio.crearInscripcion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscripcionResponse> actualizar(@PathVariable Long id,
                                                          @RequestBody @Validated InscripcionModificacionRequest request) {
        log.info("Actualizando inscripción con id: {}", id);
        InscripcionResponse response = inscripcionServicio.actualizarInscripcion(id, request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/bulk")
    public ResponseEntity<List<InscripcionResponse>> crearInscripcionesMasivas(@RequestBody List<InscripcionRegistroRequest> requests) {
        List<InscripcionResponse> responses = inscripcionServicio.crearInscripcionesMasivas(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasInscripcionResponse> obtenerEstadisticas() {
        EstadisticasInscripcionResponse estadisticas = inscripcionServicio.obtenerEstadisticas();
        return ResponseEntity.ok(estadisticas);
    }

    @GetMapping
    public ResponseEntity<List<InscripcionResponse>> listar(@RequestParam(required = false) Long alumnoId) {
        if (alumnoId != null) {
            log.info("Listando inscripciones para el alumnoId: {}", alumnoId);
            return ResponseEntity.ok(inscripcionServicio.listarPorAlumno(alumnoId));
        }
        return ResponseEntity.ok(inscripcionServicio.listarInscripciones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            InscripcionResponse response = inscripcionServicio.obtenerPorId(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error al obtener inscripción con id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/disciplina/{disciplinaId}")
    public ResponseEntity<List<InscripcionResponse>> listarPorDisciplina(@PathVariable Long disciplinaId) {
        log.info("Listando inscripciones para la disciplinaId: {}", disciplinaId);
        List<InscripcionResponse> inscripciones = inscripcionServicio.listarPorDisciplina(disciplinaId);
        return inscripciones.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(inscripciones);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            log.info("Eliminando inscripción con id: {}", id);
            inscripcionServicio.eliminarInscripcion(id);
            return ResponseEntity.ok("Inscripción eliminada exitosamente.");
        } catch (IllegalArgumentException e) {
            log.error("Error al eliminar inscripción con id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<InscripcionResponse> obtenerInscripcionActiva(@PathVariable Long alumnoId) {
        InscripcionResponse response = inscripcionServicio.obtenerInscripcionActiva(alumnoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alumno/{alumnoId}/activas")
    public ResponseEntity<List<InscripcionResponse>> obtenerInscripcionesActivas(@PathVariable Long alumnoId) {
        List<InscripcionResponse> responses = inscripcionServicio.listarPorAlumno(alumnoId);
        return ResponseEntity.ok(responses);
    }

}
