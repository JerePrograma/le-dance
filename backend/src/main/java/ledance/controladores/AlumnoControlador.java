package ledance.controladores;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.dto.response.DisciplinaResponse;
import ledance.servicios.IAlumnoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumnos")
@Validated
public class AlumnoControlador {

    private static final Logger log = LoggerFactory.getLogger(AlumnoControlador.class);
    private final IAlumnoServicio alumnoServicio;

    public AlumnoControlador(IAlumnoServicio alumnoServicio) {
        this.alumnoServicio = alumnoServicio;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@RequestBody @Validated AlumnoRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());
        AlumnoResponse response = alumnoServicio.registrarAlumno(requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarAlumnos() {
        List<AlumnoResponse> alumnos = alumnoServicio.listarAlumnos();
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtenerAlumnoPorId(@PathVariable Long id) {
        AlumnoResponse alumno = alumnoServicio.obtenerAlumnoPorId(id);
        return ResponseEntity.ok(alumno);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizarAlumno(@PathVariable Long id,
                                                           @RequestBody @Validated AlumnoRequest requestDTO) {
        AlumnoResponse alumno = alumnoServicio.actualizarAlumno(id, requestDTO);
        return ResponseEntity.ok(alumno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarAlumno(@PathVariable Long id) {
        alumnoServicio.eliminarAlumno(id);
        return ResponseEntity.ok("Alumno eliminado (baja logica) exitosamente.");
    }

    @GetMapping("/listado")
    public ResponseEntity<List<AlumnoListadoResponse>> obtenerListadoAlumnosSimplificado() {
        List<AlumnoListadoResponse> listado = alumnoServicio.listarAlumnosSimplificado();
        return ResponseEntity.ok(listado);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AlumnoListadoResponse>> buscarPorNombre(@RequestParam String nombre) {
        List<AlumnoListadoResponse> resultado = alumnoServicio.buscarPorNombre(nombre);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{alumnoId}/disciplinas")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasDeAlumno(@PathVariable Long alumnoId) {
        List<DisciplinaResponse> disciplinas = alumnoServicio.obtenerDisciplinasDeAlumno(alumnoId);
        return ResponseEntity.ok(disciplinas);
    }

}
