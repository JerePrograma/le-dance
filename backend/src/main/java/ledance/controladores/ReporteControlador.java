package ledance.controladores;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.servicios.ReporteServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteControlador {

    private final ReporteServicio reporteServicio;

    public ReporteControlador(ReporteServicio reporteServicio) {
        this.reporteServicio = reporteServicio;
    }

    @PostMapping("/generar")
    public ResponseEntity<List<ReporteResponse>> generarReporte(@RequestBody ReporteRequest request) {
        List<ReporteResponse> reporte = reporteServicio.generarReporte(request);
        return ResponseEntity.ok(reporte);
    }
}
