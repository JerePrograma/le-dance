package ledance.controladores;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;
import ledance.servicios.DisciplinaServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplinas")
public class DisciplinaControlador {

    private final DisciplinaServicio disciplinaServicio;

    public DisciplinaControlador(DisciplinaServicio disciplinaServicio) {
        this.disciplinaServicio = disciplinaServicio;
    }

    @PostMapping
    public ResponseEntity<DisciplinaResponse> crearDisciplina(@RequestBody DisciplinaRequest requestDTO) {
        return ResponseEntity.ok(disciplinaServicio.crearDisciplina(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<DisciplinaResponse>> listarDisciplinas() {
        return ResponseEntity.ok(disciplinaServicio.listarDisciplinas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaResponse> obtenerDisciplinaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(disciplinaServicio.obtenerDisciplinaPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplinaResponse> actualizarDisciplina(@PathVariable Long id,
                                                                      @RequestBody DisciplinaRequest requestDTO) {
        return ResponseEntity.ok(disciplinaServicio.actualizarDisciplina(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarDisciplina(@PathVariable Long id) {
        disciplinaServicio.eliminarDisciplina(id);
        return ResponseEntity.ok("Disciplina eliminada exitosamente.");
    }

    @GetMapping("/por-fecha")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasPorFecha(@RequestParam String fecha) {
        try {
            return ResponseEntity.ok(disciplinaServicio.obtenerDisciplinasPorFecha(fecha));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
