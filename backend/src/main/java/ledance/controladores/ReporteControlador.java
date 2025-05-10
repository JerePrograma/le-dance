package ledance.controladores;

import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.dto.reporte.request.ReporteLiquidacionRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.profesor.ProfesorServicio;
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
    private final MensualidadServicio mensualidadServicio;
    private final ProfesorServicio profesorServicio;

    public ReporteControlador(MensualidadServicio mensualidadServicio, ProfesorServicio profesorServicio) {
        this.mensualidadServicio = mensualidadServicio;
        this.profesorServicio = profesorServicio;
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
            byte[] pdf = profesorServicio.exportarLiquidacionProfesor(req);
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
