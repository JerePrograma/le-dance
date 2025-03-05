package ledance.controladores;

import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import jakarta.validation.Valid;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/disciplinas")
@Validated
public class DisciplinaControlador {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaControlador.class);
    private final DisciplinaServicio disciplinaServicio;

    public DisciplinaControlador(DisciplinaServicio disciplinaServicio) {
        this.disciplinaServicio = disciplinaServicio;
    }

    /**
     * ✅ Registrar una nueva disciplina.
     */
    @PostMapping
    public ResponseEntity<DisciplinaDetalleResponse> registrarDisciplina(@Valid @RequestBody DisciplinaRegistroRequest requestDTO) {
        log.info("Registrando disciplina: {}", requestDTO.nombre());
        DisciplinaDetalleResponse response = disciplinaServicio.crearDisciplina(requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Listar TODAS las disciplinas con detalles completos.
     */
    @GetMapping
    public ResponseEntity<List<DisciplinaDetalleResponse>> listarDisciplinas() {
        List<DisciplinaDetalleResponse> disciplinas = disciplinaServicio.listarDisciplinas();
        return ResponseEntity.ok(disciplinas);
    }

    /**
     * ✅ Obtener una disciplina por ID con detalles completos.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaDetalleResponse> obtenerDisciplinaPorId(@PathVariable Long id) {
        DisciplinaDetalleResponse disciplina = disciplinaServicio.obtenerDisciplinaPorId(id);
        return ResponseEntity.ok(disciplina);
    }

    /**
     * ✅ Actualizar una disciplina por ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DisciplinaDetalleResponse> actualizarDisciplina(@PathVariable Long id,
                                                                          @Valid @RequestBody DisciplinaModificacionRequest requestDTO) {
        log.info("Actualizando disciplina con id: {}", id);
        DisciplinaDetalleResponse disciplinaActualizada = disciplinaServicio.actualizarDisciplina(id, requestDTO);
        return ResponseEntity.ok(disciplinaActualizada);
    }

    /**
     * ✅ Dar de baja (baja logica) a una disciplina.
     */
    @DeleteMapping("/dar-baja/{id}")
    public ResponseEntity<Void> darBajaDisciplina(@PathVariable Long id) {
        log.info("Dando de baja la disciplina con id: {}", id);
        disciplinaServicio.darBajaDisciplina(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDisciplina(@PathVariable Long id) {
        log.info("Eliminando la disciplina con id: {}", id);
        disciplinaServicio.eliminarDisciplina(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * ✅ Listar disciplinas activas en formato simplificado.
     */
    @GetMapping("/listado")
    public ResponseEntity<List<DisciplinaListadoResponse>> listarDisciplinasSimplificadas() {
        List<DisciplinaListadoResponse> disciplinas = disciplinaServicio.listarDisciplinasSimplificadas();
        return disciplinas.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(disciplinas);
    }

    /**
     * ✅ Obtener disciplinas activas segun una fecha especifica.
     */
    @GetMapping("/por-fecha")
    public ResponseEntity<List<DisciplinaListadoResponse>> obtenerDisciplinasPorFecha(@RequestParam String fecha) {
        List<DisciplinaListadoResponse> disciplinas = disciplinaServicio.obtenerDisciplinasPorFecha(fecha);
        return ResponseEntity.ok(disciplinas);
    }

    /**
     * ✅ Obtener alumnos de una disciplina especifica.
     */
    @GetMapping("/{disciplinaId}/alumnos")
    public ResponseEntity<List<AlumnoListadoResponse>> obtenerAlumnosDeDisciplina(@PathVariable Long disciplinaId) {
        List<AlumnoListadoResponse> alumnos = disciplinaServicio.obtenerAlumnosDeDisciplina(disciplinaId);
        return ResponseEntity.ok(alumnos);
    }

    /**
     * ✅ Obtener el profesor de una disciplina especifica.
     */
    @GetMapping("/{disciplinaId}/profesor")
    public ResponseEntity<ProfesorListadoResponse> obtenerProfesorDeDisciplina(@PathVariable Long disciplinaId) {
        ProfesorListadoResponse profesor = disciplinaServicio.obtenerProfesorDeDisciplina(disciplinaId);
        return ResponseEntity.ok(profesor);
    }

    @GetMapping("/por-horario")
    public ResponseEntity<List<DisciplinaListadoResponse>> obtenerDisciplinasPorHorario(@RequestParam String horario) {
        LocalTime horarioInicio = LocalTime.parse(horario);
        List<DisciplinaListadoResponse> disciplinas = disciplinaServicio.obtenerDisciplinasPorHorario(horarioInicio);
        return ResponseEntity.ok(disciplinas);
    }

}
