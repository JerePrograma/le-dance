package ledance.controladores;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.servicios.AlumnoServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumnos")
public class AlumnoControlador {

    private final AlumnoServicio alumnoServicio;

    public AlumnoControlador(AlumnoServicio alumnoServicio) {
        this.alumnoServicio = alumnoServicio;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@RequestBody AlumnoRequest requestDTO) {
        return ResponseEntity.ok(alumnoServicio.registrarAlumno(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarAlumnos() {
        return ResponseEntity.ok(alumnoServicio.listarAlumnos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtenerAlumnoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoServicio.obtenerAlumnoPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizarAlumno(@PathVariable Long id,
                                                           @RequestBody AlumnoRequest requestDTO) {
        return ResponseEntity.ok(alumnoServicio.actualizarAlumno(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarAlumno(@PathVariable Long id) {
        alumnoServicio.eliminarAlumno(id);
        return ResponseEntity.ok("Alumno eliminado (baja lógica) exitosamente.");
    }

    @GetMapping("/listado")
    public ResponseEntity<List<AlumnoListadoResponse>> obtenerListadoAlumnosSimplificado() {
        return ResponseEntity.ok(alumnoServicio.listarAlumnosSimplificado());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AlumnoListadoResponse>> buscarAlumnosPorNombre(@RequestParam String nombre) {
        List<AlumnoListadoResponse> alumnos = alumnoServicio.buscarPorNombre(nombre);
        return ResponseEntity.ok(alumnos);
    }

}
