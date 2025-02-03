package ledance.controladores;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.DisciplinaResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.servicios.ProfesorServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesores")
@Validated
public class ProfesorControlador {

    private static final Logger log = LoggerFactory.getLogger(ProfesorControlador.class);
    private final ProfesorServicio profesorService;

    public ProfesorControlador(ProfesorServicio profesorService) {
        this.profesorService = profesorService;
    }

    @PostMapping
    public ResponseEntity<DatosRegistroProfesorResponse> registrarProfesor(@RequestBody @Validated ProfesorRegistroRequest request) {
        log.info("Registrando profesor: {} {}", request.nombre(), request.apellido());
        DatosRegistroProfesorResponse response = profesorService.registrarProfesor(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatosRegistroProfesorResponse> obtenerProfesorPorId(@PathVariable Long id) {
        DatosRegistroProfesorResponse response = profesorService.obtenerProfesorPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DatosRegistroProfesorResponse>> listarProfesores() {
        List<DatosRegistroProfesorResponse> respuesta = profesorService.listarProfesores();
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/asignar-usuario")
    public ResponseEntity<String> asignarUsuario(@PathVariable Long id, @RequestParam Long usuarioId) {
        profesorService.asignarUsuario(id, usuarioId);
        return ResponseEntity.ok("Usuario asignado al profesor correctamente.");
    }

    @PatchMapping("/{profesorId}/asignar-disciplina/{disciplinaId}")
    public ResponseEntity<String> asignarDisciplina(@PathVariable Long profesorId, @PathVariable Long disciplinaId) {
        profesorService.asignarDisciplina(profesorId, disciplinaId);
        return ResponseEntity.ok("Disciplina asignada al profesor correctamente.");
    }

    @GetMapping("/simplificados")
    public ResponseEntity<List<ProfesorListadoResponse>> listarProfesoresSimplificados() {
        List<ProfesorListadoResponse> respuesta = profesorService.listarProfesoresSimplificados();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{profesorId}/disciplinas")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasDeProfesor(@PathVariable Long profesorId) {
        List<DisciplinaResponse> disciplinas = profesorService.obtenerDisciplinasDeProfesor(profesorId);
        return ResponseEntity.ok(disciplinas);
    }

}
