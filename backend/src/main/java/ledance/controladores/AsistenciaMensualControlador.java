package ledance.controladores;

import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/asistencias-mensuales")
public class AsistenciaMensualControlador {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaMensualControlador.class);
    private final AsistenciaMensualServicio asistenciaMensualServicio;

    public AsistenciaMensualControlador(AsistenciaMensualServicio asistenciaMensualServicio) {
        this.asistenciaMensualServicio = asistenciaMensualServicio;
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<AsistenciaMensualDetalleResponse> obtenerAsistenciaMensual(@PathVariable Long id) {
        AsistenciaMensualDetalleResponse response = asistenciaMensualServicio.obtenerAsistenciaMensual(id);

        if (response.disciplinaId() == null) {
            log.error("⚠️ Error: La asistencia mensual no contiene un disciplinaId válido.");
            throw new IllegalArgumentException("No se encontró la disciplina en la asistencia mensual.");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AsistenciaMensualListadoResponse>> listarAsistenciasMensuales(
            @RequestParam(required = false) Long profesorId,
            @RequestParam(required = false) Long disciplinaId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        log.info("Listando asistencias mensuales con filtros: profesorId={}, disciplinaId={}, mes={}, anio={}",
                profesorId, disciplinaId, mes, anio);
        return ResponseEntity.ok(asistenciaMensualServicio.listarAsistenciasMensuales(profesorId, disciplinaId, mes, anio));
    }

    @PostMapping
    public ResponseEntity<AsistenciaMensualDetalleResponse> registrarAsistenciaMensual(
            @Valid @RequestBody AsistenciaMensualRegistroRequest request) {
        log.info("Registrando asistencia mensual para inscripcionId={} en mes={} y anio={}",
                request.inscripcionId(), request.mes(), request.anio());
        return new ResponseEntity<>(asistenciaMensualServicio.registrarAsistenciaMensual(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaMensualDetalleResponse> actualizarAsistenciaMensual(
            @PathVariable Long id, @Valid @RequestBody AsistenciaMensualModificacionRequest request) {
        log.info("Actualizando asistencia mensual con id={}", id);
        return ResponseEntity.ok(asistenciaMensualServicio.actualizarAsistenciaMensual(id, request));
    }
}
