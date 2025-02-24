package ledance.controladores;

import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pagos")
@Validated
public class PagoControlador {

    private static final Logger log = LoggerFactory.getLogger(PagoControlador.class);
    private final PagoServicio pagoServicio;

    public PagoControlador(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrarPago(@RequestBody @Validated PagoRegistroRequest request) {
        log.info("Registrando pago para inscripcionId: {}", request.inscripcionId());
        PagoResponse response = pagoServicio.registrarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtenerPagoPorId(@PathVariable Long id) {
        log.info("Obteniendo pago con id: {}", id);
        PagoResponse response = pagoServicio.obtenerPagoPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarPagos() {
        log.info("Listando todos los pagos");
        List<PagoResponse> pagos = pagoServicio.listarPagos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @GetMapping("/inscripcion/{inscripcionId}")
    public ResponseEntity<List<PagoResponse>> listarPagosPorInscripcion(@PathVariable Long inscripcionId) {
        log.info("Listando pagos para inscripcionId: {}", inscripcionId);
        List<PagoResponse> pagos = pagoServicio.listarPagosPorInscripcion(inscripcionId);
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<PagoResponse>> listarPagosPorAlumno(@PathVariable Long alumnoId) {
        log.info("Listando pagos para alumnoId: {}", alumnoId);
        List<PagoResponse> pagos = pagoServicio.listarPagosPorAlumno(alumnoId);
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<PagoResponse>> listarPagosVencidos() {
        log.info("Listando pagos vencidos");
        List<PagoResponse> pagos = pagoServicio.listarPagosVencidos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoResponse> actualizarPago(@PathVariable Long id,
                                                       @RequestBody @Validated PagoModificacionRequest request) {
        log.info("Actualizando pago con id: {}", id);
        PagoResponse response = pagoServicio.actualizarPago(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        log.info("Eliminando pago con id: {}", id);
        pagoServicio.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alumno/{alumnoId}/facturas")
    public ResponseEntity<List<PagoResponse>> listarFacturasPorAlumno(@PathVariable Long alumnoId) {
        List<PagoResponse> facturas = pagoServicio.listarPagosPorAlumno(alumnoId)
                .stream()
                .filter(p -> p.observaciones() != null && p.observaciones().contains("FACTURA"))
                .collect(Collectors.toList());
        return facturas.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(facturas);
    }

    @PutMapping("/{id}/quitar-recargo")
    public ResponseEntity<PagoResponse> quitarRecargo(@PathVariable Long id) {
        PagoResponse response = pagoServicio.quitarRecargoManual(id);
        return ResponseEntity.ok(response);
    }

    // Nuevo endpoint para obtener la cobranza consolidada de un alumno
    @GetMapping("/alumno/{alumnoId}/cobranza")
    public ResponseEntity<CobranzaDTO> obtenerCobranzaPorAlumno(@PathVariable Long alumnoId) {
        CobranzaDTO cobranza = pagoServicio.generarCobranzaPorAlumno(alumnoId);
        return ResponseEntity.ok(cobranza);
    }
}
