package ledance.controladores;

import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@Validated
public class PagoControlador {

    private static final Logger log = LoggerFactory.getLogger(PagoControlador.class);
    private final PagoServicio pagoServicio;

    public PagoControlador(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    // Registrar un nuevo pago
    @PostMapping
    public ResponseEntity<PagoResponse> registrarPago(@RequestBody @Validated PagoRegistroRequest request) {
        log.info("Registrando pago para inscripcionId: {}", request.inscripcionId());
        PagoResponse response = pagoServicio.registrarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Obtener un pago por ID
    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtenerPagoPorId(@PathVariable Long id) {
        log.info("Obteniendo pago con id: {}", id);
        PagoResponse response = pagoServicio.obtenerPagoPorId(id);
        return ResponseEntity.ok(response);
    }

    // Listar todos los pagos activos
    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarPagos() {
        log.info("Listando todos los pagos activos");
        List<PagoResponse> pagos = pagoServicio.listarPagos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    // Listar pagos por inscripción
    @GetMapping("/inscripcion/{inscripcionId}")
    public ResponseEntity<List<PagoResponse>> listarPagosPorInscripcion(@PathVariable Long inscripcionId) {
        log.info("Listando pagos para inscripcionId: {}", inscripcionId);
        List<PagoResponse> pagos = pagoServicio.listarPagosPorInscripcion(inscripcionId);
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    // Listar pagos por alumno
    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<PagoResponse>> listarPagosPorAlumno(@PathVariable Long alumnoId) {
        log.info("Listando pagos para alumnoId: {}", alumnoId);
        List<PagoResponse> pagos = pagoServicio.listarPagosPorAlumno(alumnoId);
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    // Listar pagos vencidos
    @GetMapping("/vencidos")
    public ResponseEntity<List<PagoResponse>> listarPagosVencidos() {
        log.info("Listando pagos vencidos");
        List<PagoResponse> pagos = pagoServicio.listarPagosVencidos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    // Actualizar un pago
    @PutMapping("/{id}")
    public ResponseEntity<PagoResponse> actualizarPago(@PathVariable Long id,
                                                       @RequestBody @Validated PagoModificacionRequest request) {
        log.info("Actualizando pago con id: {}", id);
        PagoResponse response = pagoServicio.actualizarPago(id, request);
        return ResponseEntity.ok(response);
    }

    // Baja lógica de un pago
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        log.info("Eliminando pago con id: {}", id);
        pagoServicio.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }
}
