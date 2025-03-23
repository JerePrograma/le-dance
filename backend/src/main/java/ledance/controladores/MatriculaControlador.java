package ledance.controladores;

import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.Matricula;
import ledance.servicios.matricula.MatriculaServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matriculas")
@Validated
public class MatriculaControlador {

    private final MatriculaServicio matriculaServicio;
    private final MatriculaMapper matriculaMapper;

    public MatriculaControlador(MatriculaServicio matriculaServicio, MatriculaMapper matriculaMapper) {
        this.matriculaServicio = matriculaServicio;
        this.matriculaMapper = matriculaMapper;
    }

    @GetMapping("/{alumnoId}")
    public ResponseEntity<MatriculaResponse> obtenerMatricula(@PathVariable Long alumnoId, int anio) {
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumnoId, anio);
        return ResponseEntity.ok(matriculaMapper.toResponse(matricula));
    }

    @PutMapping("/{matriculaId}")
    public ResponseEntity<MatriculaResponse> actualizarMatricula(
            @PathVariable Long matriculaId,
            @RequestBody MatriculaRegistroRequest request) {
        MatriculaResponse updated = matriculaServicio.actualizarEstadoMatricula(matriculaId, request);
        return ResponseEntity.ok(updated);
    }
}
