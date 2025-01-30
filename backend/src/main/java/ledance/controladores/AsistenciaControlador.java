package ledance.controladores;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.servicios.AsistenciaServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaControlador {

    private final AsistenciaServicio asistenciaServicio;

    public AsistenciaControlador(AsistenciaServicio asistenciaServicio) {
        this.asistenciaServicio = asistenciaServicio;
    }

    @PostMapping
    public ResponseEntity<AsistenciaResponseDTO> registrarAsistencia(@RequestBody AsistenciaRequest requestDTO) {
        AsistenciaResponseDTO asistencia = asistenciaServicio.registrarAsistencia(requestDTO);
        return ResponseEntity.ok(asistencia);
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
        return ResponseEntity.ok(asistenciaServicio.generarReporteAsistencias());
    }
}
