package ledance.controladores;

import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.servicios.mensualidad.MensualidadServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes/mensualidades")
@CrossOrigin(origins = "*")
public class ReporteMensualidadControlador {

    private final MensualidadServicio mensualidadServicio;

    public ReporteMensualidadControlador(MensualidadServicio mensualidadServicio) {
        this.mensualidadServicio = mensualidadServicio;
    }

    /**
     * Endpoint para buscar mensualidades con filtros.
     * Parámetros:
     * - fechaInicio (obligatorio, formato yyyy-MM-dd)
     * - fechaFin (obligatorio, formato yyyy-MM-dd)
     * - disciplinaId (opcional)
     * - profesorId (opcional)
     *
     * Se utiliza Pageable para paginación.
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ReporteMensualidadDTO>> buscarMensualidades(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String disciplinaNombre,
            @RequestParam(required = false) String profesorNombre
    ) {
        List<ReporteMensualidadDTO> resultados = mensualidadServicio.buscarMensualidades(
                fechaInicio, fechaFin, disciplinaNombre, profesorNombre
        );
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/buscar-mensualidades-alumno-por-mes")
    public ResponseEntity<List<ReporteMensualidadDTO>> buscarMensualidadesAlumnoPorMes(
            @RequestParam(name = "fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaMes,
            @RequestParam String alumnoNombre
    ) {
        List<ReporteMensualidadDTO> resultados = mensualidadServicio
                .buscarMensualidadesAlumnoPorMes(fechaMes, alumnoNombre);
        return ResponseEntity.ok(resultados);
    }

}
