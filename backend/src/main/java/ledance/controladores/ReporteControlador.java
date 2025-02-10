package ledance.controladores;

import ledance.dto.request.ReporteRegistroRequest;
import ledance.dto.response.ReporteResponse;
import ledance.servicios.ReporteServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Validated
public class ReporteControlador {

    private static final Logger log = LoggerFactory.getLogger(ReporteControlador.class);
    private final ReporteServicio reporteServicio;

    public ReporteControlador(ReporteServicio reporteServicio) {
        this.reporteServicio = reporteServicio;
    }

    /**
     * ✅ Generar un nuevo reporte basado en el tipo seleccionado.
     */
    @PostMapping("/generar")
    public ResponseEntity<ReporteResponse> generarReporte(@RequestBody @Validated ReporteRegistroRequest request) {
        log.info("Generando reporte de tipo: {}", request.tipo());
        try {
            ReporteResponse reporte = reporteServicio.generarReporte(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
        } catch (IllegalArgumentException e) {
            log.error("Error generando reporte: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error interno generando reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * ✅ Obtener un reporte específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReporteResponse> obtenerReportePorId(@PathVariable Long id) {
        log.info("Obteniendo reporte con id: {}", id);
        try {
            ReporteResponse reporte = reporteServicio.obtenerReportePorId(id);
            return ResponseEntity.ok(reporte);
        } catch (IllegalArgumentException e) {
            log.error("Error obteniendo reporte: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error interno obteniendo reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ Listar todos los reportes generados.
     */
    @GetMapping
    public ResponseEntity<List<ReporteResponse>> listarReportes() {
        log.info("Listando todos los reportes");
        try {
            List<ReporteResponse> reportes = reporteServicio.listarReportes();
            return ResponseEntity.ok(reportes);
        } catch (Exception e) {
            log.error("Error interno listando reportes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ Eliminar un reporte (baja lógica).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReporte(@PathVariable Long id) {
        log.info("Eliminando reporte con id: {}", id);
        try {
            reporteServicio.eliminarReporte(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (IllegalArgumentException e) {
            log.error("Error eliminando reporte: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error interno eliminando reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
