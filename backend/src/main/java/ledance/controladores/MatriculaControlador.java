package ledance.controladores;

import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.servicios.matricula.MatriculaServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matriculas")
@Validated
public class MatriculaControlador {

    private final MatriculaServicio matriculaServicio;

    public MatriculaControlador(MatriculaServicio matriculaServicio) {
        this.matriculaServicio = matriculaServicio;
    }

    @GetMapping("/{alumnoId}")
    public ResponseEntity<MatriculaResponse> obtenerMatricula(@PathVariable Long alumnoId) {
        MatriculaResponse matricula = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        return ResponseEntity.ok(matricula);
    }

    @PutMapping("/{matriculaId}")
    public ResponseEntity<MatriculaResponse> actualizarMatricula(
            @PathVariable Long matriculaId,
            @RequestBody MatriculaModificacionRequest request) {
        MatriculaResponse updated = matriculaServicio.actualizarEstadoMatricula(matriculaId, request);
        return ResponseEntity.ok(updated);
    }
}
