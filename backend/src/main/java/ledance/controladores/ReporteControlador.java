package ledance.controladores;

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

    @GetMapping("/recaudacion")
    public ResponseEntity<List<String>> obtenerRecaudacionPorDisciplina() {
        return ResponseEntity.ok(reporteServicio.generarReporteRecaudacionPorDisciplina());
    }

    @GetMapping("/asistencias")
    public ResponseEntity<List<String>> obtenerReporteAsistencias() {
        return ResponseEntity.ok(reporteServicio.generarReporteAsistencias());
    }
}
