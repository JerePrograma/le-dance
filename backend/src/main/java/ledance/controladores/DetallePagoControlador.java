package ledance.controladores;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import ledance.entidades.EstadoPago;
import ledance.servicios.detallepago.DetallePagoServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/detalle-pago")
public class DetallePagoControlador {

    private final DetallePagoServicio detallePagoServicio;

    public DetallePagoControlador(DetallePagoServicio detallePagoServicio) {
        this.detallePagoServicio = detallePagoServicio;
    }

    // Endpoint para crear un nuevo DetallePago
    @PostMapping
    public ResponseEntity<DetallePagoResponse> crearDetallePago(@RequestBody DetallePago detalle) {
        DetallePagoResponse response = detallePagoServicio.crearDetallePago(detalle);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Endpoint para obtener un DetallePago por su ID
    @GetMapping("/{id}")
    public ResponseEntity<DetallePagoResponse> obtenerDetallePagoPorId(@PathVariable Long id) {
        try {
            DetallePagoResponse response = detallePagoServicio.obtenerDetallePagoPorId(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para actualizar un DetallePago existente
    @PutMapping("/{id}")
    public ResponseEntity<DetallePagoResponse> actualizarDetallePago(@PathVariable Long id, @RequestBody DetallePago detalle) {
        try {
            DetallePagoResponse response = detallePagoServicio.actualizarDetallePago(id, detalle);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/anular/{id}")
    public ResponseEntity<DetallePagoResponse> anularDetallePago(@PathVariable Long id) {
        try {
            DetallePagoResponse response = detallePagoServicio.anularDetallePago(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para eliminar un DetallePago por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDetallePago(@PathVariable Long id) {
        try {
            detallePagoServicio.eliminarDetallePago(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/fecha")
    public ResponseEntity<List<DetallePagoResponse>> listarDetallesPagos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        List<DetallePagoResponse> responses = detallePagoServicio.listarDetallesPagos(fechaDesde, fechaHasta);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<DetallePagoResponse>> listarPorAlumno(@PathVariable Long alumnoId) {
        List<DetallePagoResponse> responses = detallePagoServicio.listarPorAlumno(alumnoId);
        return ResponseEntity.ok(responses);
    }
}
