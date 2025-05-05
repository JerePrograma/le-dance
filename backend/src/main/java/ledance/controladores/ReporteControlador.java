package ledance.controladores;

import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.dto.reporte.request.ReporteLiquidacionRequest;
import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.reporte.ReporteServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Validated
public class ReporteControlador {

    private static final Logger log = LoggerFactory.getLogger(ReporteControlador.class);
    private final ReporteServicio reporteServicio;
    private final MensualidadServicio mensualidadServicio;

    public ReporteControlador(ReporteServicio reporteServicio, MensualidadServicio mensualidadServicio) {
        this.reporteServicio = reporteServicio;
        this.mensualidadServicio = mensualidadServicio;
    }

    // Endpoint generico de generacion (si fuera necesario)
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

    // Endpoint para Recaudacion por Disciplina
    @GetMapping("/recaudacion-disciplina")
    public ResponseEntity<ReporteResponse> generarReporteRecaudacionPorDisciplina(
            @RequestParam Long disciplinaId,
            @RequestParam(required = false) Long usuarioId) {
        try {
            ReporteResponse reporte = reporteServicio.generarReporteRecaudacionPorDisciplina(disciplinaId, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
        } catch (Exception e) {
            log.error("Error generando reporte de recaudacion por disciplina: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para Asistencias por Alumno
    @GetMapping("/asistencias-alumno")
    public ResponseEntity<ReporteResponse> generarReporteAsistenciasPorAlumno(
            @RequestParam Long alumnoId,
            @RequestParam(required = false) Long usuarioId) {
        try {
            ReporteResponse reporte = reporteServicio.generarReporteAsistenciasPorAlumno(alumnoId, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
        } catch (Exception e) {
            log.error("Error generando reporte de asistencias por alumno: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para Asistencias por Disciplina
    @GetMapping("/asistencias-disciplina")
    public ResponseEntity<ReporteResponse> generarReporteAsistenciasPorDisciplina(
            @RequestParam Long disciplinaId,
            @RequestParam(required = false) Long usuarioId) {
        try {
            ReporteResponse reporte = reporteServicio.generarReporteAsistenciasPorDisciplina(disciplinaId, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
        } catch (Exception e) {
            log.error("Error generando reporte de asistencias por disciplina: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para Asistencias por Disciplina y Alumno
    @GetMapping("/asistencias-disciplina-alumno")
    public ResponseEntity<ReporteResponse> generarReporteAsistenciasPorDisciplinaAlumno(
            @RequestParam Long disciplinaId,
            @RequestParam Long alumnoId,
            @RequestParam(required = false) Long usuarioId) {
        try {
            ReporteResponse reporte = reporteServicio.generarReporteAsistenciasPorDisciplinaAlumno(disciplinaId, alumnoId, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
        } catch (Exception e) {
            log.error("Error generando reporte de asistencias por disciplina y alumno: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Listar, obtener por ID y eliminar reportes (metodos ya existentes)
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReporte(@PathVariable Long id) {
        log.info("Eliminando reporte con id: {}", id);
        try {
            reporteServicio.eliminarReporte(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error eliminando reporte: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error interno eliminando reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<ReporteResponse>> buscarReportes(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Pageable pageable) {
        Page<ReporteResponse> reportes = reporteServicio.generarReportePaginado(usuarioId, tipo, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(reportes);
    }

    // --- 1) CONTROLADOR ---
    @PostMapping("/mensualidades/exportar")
    public ResponseEntity<byte[]> exportarLiquidacionProfesor(
            @RequestBody @Validated ReporteLiquidacionRequest req
    ) {
        log.info("Exportando liquidación de '{}' ({}→{}) al {}% sobre disciplina '{}', detalles recibidos={}",
                req.profesor(),
                req.fechaInicio(), req.fechaFin(),
                req.porcentaje(),
                req.disciplina(),
                req.detalles().size()
        );
        try {
            byte[] pdf = reporteServicio.exportarLiquidacionProfesor(req);
            String filename = "liquidacion_" + req.profesor().replace(" ", "_") + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error exportando liquidación:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para buscar mensualidades con filtros.
     * Parametros:
     * - fechaInicio (obligatorio, formato yyyy-MM-dd)
     * - fechaFin (obligatorio, formato yyyy-MM-dd)
     * - disciplinaId (opcional)
     * - profesorId (opcional)
     * <p>
     * Se utiliza Pageable para paginacion.
     */
    @GetMapping("/mensualidades/buscar")
    public ResponseEntity<List<DetallePagoResponse>> buscarMensualidades(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String disciplinaNombre,
            @RequestParam(required = false) String profesorNombre
    ) {
        List<DetallePagoResponse> resultados = mensualidadServicio.buscarDetallePagosPorDisciplina(disciplinaNombre, fechaInicio, fechaFin, profesorNombre);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/mensualidades/buscar-mensualidades-alumno-por-mes")
    public ResponseEntity<List<ReporteMensualidadDTO>> buscarMensualidadesAlumnoPorMes(
            @RequestParam(name = "fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaMes,
            @RequestParam String alumnoNombre
    ) {
        List<ReporteMensualidadDTO> resultados = mensualidadServicio
                .buscarMensualidadesAlumnoPorMes(fechaMes, alumnoNombre);
        return ResponseEntity.ok(resultados);
    }

}
