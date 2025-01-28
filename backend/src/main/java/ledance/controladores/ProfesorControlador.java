package ledance.controladores;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.servicios.ProfesorServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesores")
public class ProfesorControlador {

    private final ProfesorServicio profesorServicio;

    public ProfesorControlador(ProfesorServicio profesorServicio) {
        this.profesorServicio = profesorServicio;
    }

    /**
     * Registra un nuevo profesor.
     *
     * @param request Datos del profesor a registrar.
     * @return El profesor registrado.
     */
    @PostMapping
    public ResponseEntity<DatosRegistroProfesorResponse> registrarProfesor(@RequestBody ProfesorRegistroRequest request) {
        try {
            DatosRegistroProfesorResponse response = profesorServicio.registrarProfesor(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Obtiene un profesor por su ID.
     *
     * @param id ID del profesor.
     * @return Los datos del profesor.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatosRegistroProfesorResponse> obtenerProfesorPorId(@PathVariable Long id) {
        try {
            DatosRegistroProfesorResponse response = profesorServicio.obtenerProfesorPorId(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Lista todos los profesores.
     *
     * @return Lista de profesores.
     */
    @GetMapping
    public ResponseEntity<List<DatosRegistroProfesorResponse>> listarProfesores() {
        try {
            List<DatosRegistroProfesorResponse> response = profesorServicio.listarProfesores();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Asigna un usuario a un profesor existente.
     *
     * @param id        ID del profesor.
     * @param usuarioId ID del usuario a asignar.
     * @return Mensaje de exito o error.
     */
    @PatchMapping("/{id}/asignar-usuario")
    public ResponseEntity<String> asignarUsuario(@PathVariable Long id, @RequestParam Long usuarioId) {
        try {
            profesorServicio.asignarUsuario(id, usuarioId);
            return ResponseEntity.ok("Usuario asignado al profesor correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    /**
     * Asigna un profesor a una disciplina.
     *
     * @param profesorId   ID del profesor.
     * @param disciplinaId ID de la disciplina.
     * @return Mensaje de exito o error.
     */
    @PatchMapping("/{profesorId}/asignar-disciplina/{disciplinaId}")
    public ResponseEntity<String> asignarDisciplina(@PathVariable Long profesorId, @PathVariable Long disciplinaId) {
        try {
            profesorServicio.asignarDisciplina(profesorId, disciplinaId);
            return ResponseEntity.ok("Disciplina asignada al profesor correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor.");
        }
    }

    @GetMapping("/simplificados")
    public ResponseEntity<List<ProfesorListadoResponse>> listarProfesoresSimplificados() {
        List<ProfesorListadoResponse> profesores = profesorServicio.listarProfesoresSimplificados();
        return ResponseEntity.ok(profesores);
    }

}
