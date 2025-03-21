package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDataResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.servicios.alumno.AlumnoServicio;
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
    private final AlumnoServicio alumnoServicio;

    public AlumnoControlador(AlumnoServicio alumnoServicio) {
        this.alumnoServicio = alumnoServicio;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@Valid @RequestBody AlumnoRegistroRequest requestDTO) {
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
                                                           @RequestBody @Validated AlumnoRegistroRequest requestDTO) {
        AlumnoResponse alumno = alumnoServicio.actualizarAlumno(id, requestDTO);
        return ResponseEntity.ok(alumno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAlumno(@PathVariable Long id) {
        log.info("Eliminando al alumno con id: {}", id);
        alumnoServicio.eliminarAlumno(id);
        return ResponseEntity.noContent().build(); // ✅ 204 No Content
    }

    @DeleteMapping("/dar-baja/{id}")
    public ResponseEntity<Void> darBajaAlumno(@PathVariable Long id) {
        log.info("Dando de baja al alumno con id: {}", id);
        alumnoServicio.darBajaAlumno(id);
        return ResponseEntity.noContent().build(); // ✅ 204 No Content
    }

    @GetMapping("/listado")
    public ResponseEntity<List<AlumnoResponse>> obtenerListadoAlumnosSimplificado() {
        List<AlumnoResponse> listado = alumnoServicio.listarAlumnosSimplificado();
        return ResponseEntity.ok(listado);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AlumnoResponse>> buscarPorNombre(@RequestParam String nombre) {
        List<AlumnoResponse> resultado = alumnoServicio.buscarPorNombre(nombre);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{alumnoId}/disciplinas")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasDeAlumno(@PathVariable Long alumnoId) {
        List<DisciplinaResponse> disciplinas = alumnoServicio.obtenerDisciplinasDeAlumno(alumnoId);
        return ResponseEntity.ok(disciplinas);
    }

    @GetMapping("/{id}/datos")
    public ResponseEntity<AlumnoDataResponse> obtenerDatosAlumno(@PathVariable Long id) {
        AlumnoDataResponse response = alumnoServicio.obtenerAlumnoData(id);
        return ResponseEntity.ok(response);
    }

}
