// src/main/java/ledance/controladores/MetodoPagoControlador.java
package ledance.controladores;

import ledance.dto.metodopago.request.MetodoPagoModificacionRequest;
import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.servicios.pago.MetodoPagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metodos-pago")
@Validated
public class MetodoPagoControlador {

    private static final Logger log = LoggerFactory.getLogger(MetodoPagoControlador.class);
    private final MetodoPagoServicio metodoPagoServicio;

    public MetodoPagoControlador(MetodoPagoServicio metodoPagoServicio) {
        this.metodoPagoServicio = metodoPagoServicio;
    }

    /**
     * Registra un nuevo método de pago.
     */
    @PostMapping
    public ResponseEntity<MetodoPagoResponse> registrar(@RequestBody @Validated MetodoPagoRegistroRequest request) {
        log.info("Registrando método de pago: {}", request.descripcion());
        MetodoPagoResponse response = metodoPagoServicio.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos los métodos de pago.
     */
    @GetMapping
    public ResponseEntity<List<MetodoPagoResponse>> listar() {
        log.info("Listando métodos de pago");
        List<MetodoPagoResponse> responses = metodoPagoServicio.listar();
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un método de pago por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MetodoPagoResponse> obtenerPorId(@PathVariable Long id) {
        log.info("Obteniendo método de pago con ID: {}", id);
        MetodoPagoResponse response = metodoPagoServicio.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza un método de pago existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MetodoPagoResponse> actualizar(@PathVariable Long id,
                                                         @RequestBody @Validated MetodoPagoModificacionRequest request) {
        log.info("Actualizando método de pago con ID: {}", id);
        MetodoPagoResponse response = metodoPagoServicio.actualizar(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Realiza la baja lógica de un método de pago.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando (baja lógica) método de pago con ID: {}", id);
        metodoPagoServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
