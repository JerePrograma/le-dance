package ledance.controladores;

import ledance.dto.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.response.AsistenciaDiariaResponse;
import ledance.servicios.AsistenciaDiariaServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias-diarias")
public class AsistenciaDiariaControlador {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaDiariaControlador.class);
    private final AsistenciaDiariaServicio asistenciaDiariaServicio;

    public AsistenciaDiariaControlador(AsistenciaDiariaServicio asistenciaDiariaServicio) {
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
    }

    @PostMapping
    public ResponseEntity<AsistenciaDiariaResponse> registrarAsistencia(
            @Valid @RequestBody AsistenciaDiariaRegistroRequest request) {
        log.info("Registrando asistencia para alumnoId={} en fecha={}", request.alumnoId(), request.fecha());
        return ResponseEntity.ok(asistenciaDiariaServicio.registrarOActualizarAsistencia(request));
    }

    @PostMapping("/lote")
    public ResponseEntity<List<AsistenciaDiariaResponse>> registrarAsistenciasEnLote(
            @Valid @RequestBody List<AsistenciaDiariaRegistroRequest> requests) {
        log.info("Registrando {} asistencias en lote.", requests.size());
        return ResponseEntity.ok(asistenciaDiariaServicio.registrarAsistenciasEnLote(requests));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaDiariaResponse> modificarAsistencia(
            @PathVariable Long id, @Valid @RequestBody AsistenciaDiariaModificacionRequest request) {
        log.info("Modificando asistencia con id={} para fecha={}", id, request.fecha());
        return ResponseEntity.ok(asistenciaDiariaServicio.actualizarAsistencia(id, request));
    }

    @GetMapping("/por-asistencia-mensual/{asistenciaMensualId}")
    public ResponseEntity<List<AsistenciaDiariaResponse>> obtenerAsistenciasPorAsistenciaMensual(
            @PathVariable Long asistenciaMensualId) {
        log.info("Obteniendo asistencias diarias para asistenciaMensualId={}", asistenciaMensualId);
        return ResponseEntity.ok(asistenciaDiariaServicio.obtenerAsistenciasPorAsistenciaMensual(asistenciaMensualId));
    }

    @GetMapping("/por-disciplina-y-fecha")
    public ResponseEntity<Page<AsistenciaDiariaResponse>> obtenerAsistenciasPorDisciplinaYFecha(
            @RequestParam Long disciplinaId, @RequestParam String fecha, Pageable pageable) {
        LocalDate fechaParsed = LocalDate.parse(fecha);
        log.info("Obteniendo asistencias para disciplinaId={} en fecha={}", disciplinaId, fechaParsed);
        return ResponseEntity.ok(asistenciaDiariaServicio.obtenerAsistenciasPorDisciplinaYFecha(disciplinaId, fechaParsed, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAsistencia(@PathVariable Long id) {
        log.warn("Eliminando asistencia con id={}", id);
        asistenciaDiariaServicio.eliminarAsistencia(id);
        return ResponseEntity.noContent().build();
    }
}
