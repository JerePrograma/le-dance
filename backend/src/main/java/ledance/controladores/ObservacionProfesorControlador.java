package ledance.controladores;

import ledance.dto.reporte.observacion.ObservacionProfesorDTO;
import ledance.servicios.observaciones.ObservacionProfesorServicio;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/observaciones-profesores")
public class ObservacionProfesorControlador {

    private final ObservacionProfesorServicio observacionProfesorServicio;

    public ObservacionProfesorControlador(ObservacionProfesorServicio observacionProfesorServicio) {
        this.observacionProfesorServicio = observacionProfesorServicio;
    }

    /**
     * Crea una nueva observación para un profesor.
     *
     * @param profesorId  el id del profesor
     * @param fecha       la fecha de la observación (en formato ISO, por ejemplo "2025-03-27")
     * @param observacion el texto de la observación (en el body)
     * @return el DTO de la observación creada
     */
    @PostMapping
    public ObservacionProfesorDTO crearObservacion(
            @RequestParam Long profesorId,
            @RequestParam String fecha,
            @RequestBody String observacion) {

        LocalDate localDate = LocalDate.parse(fecha);
        return observacionProfesorServicio.crearObservacion(profesorId, localDate, observacion);
    }

    /**
     * Actualiza una observación existente.
     *
     * @param id          el id de la observación a actualizar
     * @param fecha       la nueva fecha de la observación (formato ISO)
     * @param observacion el nuevo texto de la observación (en el body)
     * @return el DTO de la observación actualizada
     */
    @PutMapping("/{id}")
    public ObservacionProfesorDTO actualizarObservacion(
            @PathVariable Long id,
            @RequestParam String fecha,
            @RequestBody String observacion) {

        LocalDate localDate = LocalDate.parse(fecha);
        return observacionProfesorServicio.actualizarObservacion(id, localDate, observacion);
    }

    /**
     * Elimina una observación.
     *
     * @param id el id de la observación a eliminar
     */
    @DeleteMapping("/{id}")
    public void eliminarObservacion(@PathVariable Long id) {
        observacionProfesorServicio.eliminarObservacion(id);
    }

    /**
     * Obtiene una observación por su id.
     *
     * @param id el id de la observación
     * @return el DTO de la observación encontrada
     */
    @GetMapping("/{id}")
    public ObservacionProfesorDTO obtenerObservacion(@PathVariable Long id) {
        return observacionProfesorServicio.obtenerObservacion(id);
    }

    /**
     * Lista todas las observaciones.
     *
     * @return la lista de DTOs de observaciones
     */
    @GetMapping
    public List<ObservacionProfesorDTO> listarObservaciones() {
        return observacionProfesorServicio.listarObservaciones();
    }

    /**
     * Lista las observaciones de un profesor.
     *
     * @param profesorId el id del profesor
     * @return la lista de DTOs de observaciones del profesor
     */
    @GetMapping("/profesor/{profesorId}")
    public List<ObservacionProfesorDTO> listarObservacionesPorProfesor(@PathVariable Long profesorId) {
        return observacionProfesorServicio.listarObservacionesPorProfesor(profesorId);
    }

    /**
     * Lista las observaciones en un rango de fechas.
     *
     * @param inicio fecha de inicio (formato ISO)
     * @param fin    fecha de fin (formato ISO)
     * @return la lista de DTOs de observaciones en el rango indicado
     */
    @GetMapping("/fechas")
    public List<ObservacionProfesorDTO> listarObservacionesEntreFechas(
            @RequestParam String inicio,
            @RequestParam String fin) {

        LocalDate fechaInicio = LocalDate.parse(inicio);
        LocalDate fechaFin = LocalDate.parse(fin);
        return observacionProfesorServicio.listarObservacionesEntreFechas(fechaInicio, fechaFin);
    }

    /**
     * Lista las observaciones de un profesor en un mes específico.
     *
     * @param profesorId el id del profesor
     * @param mes        el mes (1-12)
     * @param anio       el año
     * @return la lista de DTOs de observaciones para ese profesor y mes
     */
    @GetMapping("/profesor/{profesorId}/mes")
    public List<ObservacionProfesorDTO> listarObservacionesMensuales(
            @PathVariable Long profesorId,
            @RequestParam int mes,
            @RequestParam int anio) {

        return observacionProfesorServicio.listarObservacionesMensuales(profesorId, mes, anio);
    }
}
