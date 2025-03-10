package ledance.controladores;

import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.profesor.response.ProfesorDetalleResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.servicios.profesor.ProfesorServicio;
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
    private final ProfesorServicio profesorServicio;

    public ProfesorControlador(ProfesorServicio profesorServicio) {
        this.profesorServicio = profesorServicio;
    }

    /**
     * ✅ Registrar un nuevo profesor.
     */
    @PostMapping
    public ResponseEntity<ProfesorDetalleResponse> registrarProfesor(@RequestBody @Validated ProfesorRegistroRequest request) {
        log.info("Registrando profesor: {} {}", request.nombre(), request.apellido());
        ProfesorDetalleResponse response = profesorServicio.registrarProfesor(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Obtener un profesor por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfesorDetalleResponse> obtenerProfesorPorId(@PathVariable Long id) {
        ProfesorDetalleResponse response = profesorServicio.obtenerProfesorPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Listar todos los profesores.
     */
    @GetMapping
    public ResponseEntity<List<ProfesorListadoResponse>> listarProfesores() {
        List<ProfesorListadoResponse> profesores = profesorServicio.listarProfesores();
        return ResponseEntity.ok(profesores);
    }

    /**
     * ✅ Listar profesores activos.
     */
    @GetMapping("/activos")
    public ResponseEntity<List<ProfesorListadoResponse>> listarProfesoresActivos() {
        List<ProfesorListadoResponse> profesores = profesorServicio.listarProfesoresActivos();
        return ResponseEntity.ok(profesores);
    }

    /**
     * ✅ Actualizar un profesor.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfesorDetalleResponse> actualizarProfesor(@PathVariable Long id,
                                                                      @RequestBody @Validated ProfesorModificacionRequest request) {
        log.info("Actualizando profesor con id: {}", id);
        ProfesorDetalleResponse response = profesorServicio.actualizarProfesor(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Eliminar un profesor (baja logica).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProfesor(@PathVariable Long id) {
        log.info("Eliminando profesor con id: {}", id);
        profesorServicio.eliminarProfesor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{profesorId}/disciplinas")
    public ResponseEntity<List<DisciplinaListadoResponse>> obtenerDisciplinasDeProfesor(@PathVariable Long profesorId) {
        List<DisciplinaListadoResponse> disciplinas = profesorServicio.obtenerDisciplinasDeProfesor(profesorId);
        return ResponseEntity.ok(disciplinas);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProfesorListadoResponse>> buscarPorNombre(@RequestParam String nombre) {
        List<ProfesorListadoResponse> resultado = profesorServicio.buscarPorNombre(nombre);
        return ResponseEntity.ok(resultado);
    }
}
