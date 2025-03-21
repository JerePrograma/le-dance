package ledance.controladores;

import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deudas")
public class DeudaControlador {

    private static final Logger log = LoggerFactory.getLogger(DeudaControlador.class);
    private final PagoServicio pagoServicio;

    public DeudaControlador(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

//    @GetMapping("/alumno/{alumnoId}")
//    public ResponseEntity<DeudasPendientesResponse> obtenerDeudasPendientes(@PathVariable Long alumnoId) {
//        log.info("Consultando deudas pendientes para alumno id: {}", alumnoId);
//        DeudasPendientesResponse respuesta = pagoServicio.listarDeudasPendientesPorAlumno(alumnoId);
//        return ResponseEntity.ok(respuesta);
//    }

}
