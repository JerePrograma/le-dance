package ledance.controladores;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.servicios.ReporteServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Validated
public class ReporteControlador {

    private final ReporteServicio reporteService;

    public ReporteControlador(ReporteServicio reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping("/generar")
    public ResponseEntity<List<ReporteResponse>> generarReporte(@RequestBody @Validated ReporteRequest request) {
        List<ReporteResponse> reporte = reporteService.generarReporte(request);
        return ResponseEntity.ok(reporte);
    }
}
