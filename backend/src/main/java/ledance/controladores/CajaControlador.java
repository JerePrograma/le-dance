package ledance.controladores;

import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.RendicionDTO;
import ledance.entidades.Egreso;
import ledance.entidades.MetodoPago;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.servicios.caja.CajaServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaControlador {

    private final CajaServicio cajaServicio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;

    public CajaControlador(CajaServicio cajaServicio,
                           MetodoPagoRepositorio metodoPagoRepositorio) {
        this.cajaServicio = cajaServicio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
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
     * 3) Agregar Egreso en la fecha dada.
     *    En lugar de mandarlo por query params, se puede mandar un JSON.
     *    En este ejemplo, se mandan via query (monto, observaciones).
     */
    @PostMapping("/dia/{fecha}/egresos")
    public Egreso agregarEgreso(
            @PathVariable("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Double monto,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false, defaultValue = "1") Long metodoPagoId
    ) {
        // Obtenemos el metodo de pago (por defecto ID=1 => EFECTIVO, por ejemplo)
        MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado"));

        return cajaServicio.agregarEgreso(fecha, monto, observaciones, metodo);
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

    // Opcional: endpoints para eliminar/actualizar egresos si deseas
    // anularEgreso(...)
    // actualizarEgreso(...)
}
