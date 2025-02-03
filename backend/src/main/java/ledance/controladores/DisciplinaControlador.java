package ledance.controladores;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.servicios.DisciplinaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplinas")
@Validated
public class DisciplinaControlador {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaControlador.class);
    private final DisciplinaServicio disciplinaService;

    public DisciplinaControlador(DisciplinaServicio disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @PostMapping
    public ResponseEntity<DisciplinaResponse> crearDisciplina(@RequestBody @Validated DisciplinaRequest requestDTO) {
        log.info("Creando disciplina: {}", requestDTO.nombre());
        DisciplinaResponse response = disciplinaService.crearDisciplina(requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DisciplinaResponse>> listarDisciplinas() {
        List<DisciplinaResponse> respuesta = disciplinaService.listarDisciplinas();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaResponse> obtenerDisciplinaPorId(@PathVariable Long id) {
        DisciplinaResponse response = disciplinaService.obtenerDisciplinaPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplinaResponse> actualizarDisciplina(@PathVariable Long id,
                                                                   @RequestBody @Validated DisciplinaRequest requestDTO) {
        DisciplinaResponse response = disciplinaService.actualizarDisciplina(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarDisciplina(@PathVariable Long id) {
        disciplinaService.eliminarDisciplina(id);
        return ResponseEntity.ok("Disciplina eliminada exitosamente.");
    }

    @GetMapping("/por-fecha")
    public ResponseEntity<List<DisciplinaResponse>> obtenerDisciplinasPorFecha(@RequestParam String fecha) {
        try {
            List<DisciplinaResponse> respuesta = disciplinaService.obtenerDisciplinasPorFecha(fecha);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("Error obteniendo disciplinas por fecha: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{disciplinaId}/alumnos")
    public ResponseEntity<List<AlumnoListadoResponse>> obtenerAlumnosDeDisciplina(@PathVariable Long disciplinaId) {
        List<AlumnoListadoResponse> alumnos = disciplinaService.obtenerAlumnosDeDisciplina(disciplinaId);
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/{disciplinaId}/profesores")
    public ResponseEntity<List<ProfesorListadoResponse>> obtenerProfesoresDeDisciplina(@PathVariable Long disciplinaId) {
        List<ProfesorListadoResponse> profesores = disciplinaService.obtenerProfesoresDeDisciplina(disciplinaId);
        return ResponseEntity.ok(profesores);
    }

}
