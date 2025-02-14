package ledance.controladores;

import ledance.dto.caja.request.CajaRegistroRequest;
import ledance.dto.caja.request.CajaModificacionRequest;
import ledance.dto.caja.response.CajaResponse;
import ledance.servicios.caja.CajaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cajas")
@Validated
public class CajaControlador {

    private static final Logger log = LoggerFactory.getLogger(CajaControlador.class);
    private final CajaServicio cajaServicio;

    public CajaControlador(CajaServicio cajaServicio) {
        this.cajaServicio = cajaServicio;
    }

    @PostMapping
    public ResponseEntity<CajaResponse> registrarCaja(@RequestBody @Validated CajaRegistroRequest request) {
        log.info("Registrando nuevo movimiento de caja para la fecha: {}", request.fecha());
        CajaResponse response = cajaServicio.registrarCaja(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CajaResponse> actualizarCaja(@PathVariable Long id, @RequestBody @Validated CajaModificacionRequest request) {
        log.info("Actualizando caja con id: {}", id);
        CajaResponse response = cajaServicio.actualizarCaja(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CajaResponse> obtenerCajaPorId(@PathVariable Long id) {
        log.info("Obteniendo caja con id: {}", id);
        CajaResponse response = cajaServicio.obtenerCajaPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CajaResponse>> listarCajas(Pageable pageable) {
        Page<CajaResponse> response = cajaServicio.listarCajas(pageable);
        return ResponseEntity.ok(response);
    }

    // Endpoint adicional para listar cajas por fecha
    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<Page<CajaResponse>> listarCajasPorFecha(@PathVariable String fecha, Pageable pageable) {
        // Se debe parsear la fecha (formato YYYY-MM-DD)
        LocalDate targetDate = LocalDate.parse(fecha);
        Page<CajaResponse> response = cajaServicio.listarCajasPorFecha(targetDate, pageable);
        return ResponseEntity.ok(response);
    }
}
