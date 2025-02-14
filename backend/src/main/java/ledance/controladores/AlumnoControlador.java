package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.alumno.request.AlumnoModificacionRequest;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDetalleResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
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
    public ResponseEntity<AlumnoDetalleResponse> registrarAlumno(@Valid @RequestBody AlumnoRegistroRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());
        AlumnoDetalleResponse response = alumnoServicio.registrarAlumno(requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AlumnoListadoResponse>> listarAlumnos() {
        List<AlumnoListadoResponse> alumnos = alumnoServicio.listarAlumnos();
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoDetalleResponse> obtenerAlumnoPorId(@PathVariable Long id) {
        AlumnoDetalleResponse alumno = alumnoServicio.obtenerAlumnoPorId(id);
        return ResponseEntity.ok(alumno);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoDetalleResponse> actualizarAlumno(@PathVariable Long id,
                                                                  @RequestBody @Validated AlumnoModificacionRequest requestDTO) {
        AlumnoDetalleResponse alumno = alumnoServicio.actualizarAlumno(id, requestDTO);
        return ResponseEntity.ok(alumno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darBajaAlumno(@PathVariable Long id) {
        log.info("Dando de baja al alumno con id: {}", id);
        alumnoServicio.darBajaAlumno(id);
        return ResponseEntity.noContent().build(); // ✅ 204 No Content
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
    public ResponseEntity<List<DisciplinaListadoResponse>> obtenerDisciplinasDeAlumno(@PathVariable Long alumnoId) {
        List<DisciplinaListadoResponse> disciplinas = alumnoServicio.obtenerDisciplinasDeAlumno(alumnoId);
        return ResponseEntity.ok(disciplinas);
    }

}
