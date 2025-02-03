package ledance.controladores;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.servicios.AsistenciaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
@Validated
public class AsistenciaControlador {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaControlador.class);
    private final AsistenciaServicio asistenciaServicio;

    public AsistenciaControlador(AsistenciaServicio asistenciaServicio) {
        this.asistenciaServicio = asistenciaServicio;
    }

    @GetMapping
    public ResponseEntity<List<AsistenciaResponseDTO>> listarTodasAsistencias() {
        List<AsistenciaResponseDTO> asistencias = asistenciaServicio.listarTodasAsistencias();
        return ResponseEntity.ok(asistencias);
    }


    @PostMapping
    public ResponseEntity<AsistenciaResponseDTO> registrarAsistencia(@RequestBody @Validated AsistenciaRequest requestDTO) {
        log.info("Registrando asistencia para alumnoId: {} en disciplinaId: {}", requestDTO.alumnoId(), requestDTO.disciplinaId());
        AsistenciaResponseDTO respuesta = asistenciaServicio.registrarAsistencia(requestDTO);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/disciplina/{disciplinaId}")
    public ResponseEntity<List<AsistenciaResponseDTO>> obtenerAsistenciasPorDisciplina(@PathVariable Long disciplinaId) {
        List<AsistenciaResponseDTO> asistencias = asistenciaServicio.obtenerAsistenciasPorDisciplina(disciplinaId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<AsistenciaResponseDTO>> obtenerAsistenciasPorAlumno(@PathVariable Long alumnoId) {
        List<AsistenciaResponseDTO> asistencias = asistenciaServicio.obtenerAsistenciasPorAlumno(alumnoId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping("/fecha")
    public ResponseEntity<List<AsistenciaResponseDTO>> obtenerAsistenciasPorFechaYDisciplina(
            @RequestParam LocalDate fecha,
            @RequestParam Long disciplinaId) {
        List<AsistenciaResponseDTO> asistencias = asistenciaServicio.obtenerAsistenciasPorFechaYDisciplina(fecha, disciplinaId);
        return ResponseEntity.ok(asistencias);
    }

    @GetMapping("/reporte")
    public ResponseEntity<List<String>> obtenerReporteAsistencias() {
        List<String> reporte = asistenciaServicio.generarReporteAsistencias();
        return ResponseEntity.ok(reporte);
    }
}
