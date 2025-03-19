package ledance.controladores;

import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.RendicionDTO;
import ledance.dto.caja.response.CobranzasDataResponse;
import ledance.servicios.caja.CajaServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaControlador {

    private final CajaServicio cajaServicio;

    public CajaControlador(CajaServicio cajaServicio) {
        this.cajaServicio = cajaServicio;
    }

    /**
     * 1) Planilla general de caja en un rango
     */
    @GetMapping("/planilla")
    public List<CajaDiariaDTO> obtenerPlanilla(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return cajaServicio.obtenerPlanillaGeneral(start, end);
    }

    /**
     * 2) Caja diaria (detalle) para una fecha dada
     */
    @GetMapping("/dia/{fecha}")
    public CajaDetalleDTO obtenerCajaDia(
            @PathVariable("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return cajaServicio.obtenerCajaDiaria(fecha);
    }

    /**
     * 4) Rendicion general en un rango de fechas
     */
    @GetMapping("/rendicion")
    public RendicionDTO obtenerRendicion(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return cajaServicio.obtenerRendicionGeneral(start, end);
    }

    /**
     * NUEVO: Endpoint para generar la rendicion mensual para el mes vigente.
     * Se determina el primer y último día del mes actual.
     */
    @PostMapping("/rendicion/generar")
    public RendicionDTO generarRendicionMensual() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return cajaServicio.obtenerRendicionGeneral(start, end);
    }

    @GetMapping("/datos-unificados")
    public ResponseEntity<CobranzasDataResponse> obtenerDatosCobranzas() {
        CobranzasDataResponse response = cajaServicio.obtenerDatosCobranzas();
        return ResponseEntity.ok(response);
    }

}
