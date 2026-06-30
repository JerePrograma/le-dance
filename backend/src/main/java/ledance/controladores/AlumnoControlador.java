package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.PageResponse;
import ledance.servicios.alumno.AlumnoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

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
    public ResponseEntity<PageResponse<AlumnoResponse>> listarAlumnos(
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(alumnoServicio.listarAlumnos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtenerAlumnoPorId(@PathVariable Long id) {
        AlumnoResponse alumno = alumnoServicio.obtenerAlumnoPorId(id);
        return ResponseEntity.ok(alumno);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizarAlumno(@PathVariable Long id,
                                                           @RequestBody AlumnoRegistroRequest requestDTO) {
        AlumnoResponse alumno = alumnoServicio.actualizarAlumno(id, requestDTO);
        return ResponseEntity.ok(alumno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darBajaAlumno(@PathVariable Long id) {
        log.info("Dando de baja al alumno con id: {}", id);
        alumnoServicio.darBajaAlumno(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<PageResponse<AlumnoResponse>> buscarPorNombre(
            @RequestParam String nombre,
            @PageableDefault(size = 50, sort = {"apellido", "nombre"}) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(alumnoServicio.buscarPorNombre(nombre, pageable)));
    }

    @GetMapping("/{alumnoId}/disciplinas")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasDeAlumno(@PathVariable Long alumnoId) {
        List<DisciplinaResponse> disciplinas = alumnoServicio.obtenerDisciplinasDeAlumno(alumnoId);
        return ResponseEntity.ok(disciplinas);
    }

}
