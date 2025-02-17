package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.stock.request.TipoStockModificacionRequest;
import ledance.dto.stock.request.TipoStockRegistroRequest;
import ledance.dto.stock.response.TipoStockResponse;
import ledance.servicios.stock.TipoStockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tipo-stocks")
@Validated
public class TipoStockControlador {

    private static final Logger log = LoggerFactory.getLogger(TipoStockControlador.class);
    private final TipoStockServicio tipoStockServicio;

    public TipoStockControlador(TipoStockServicio tipoStockServicio) {
        this.tipoStockServicio = tipoStockServicio;
    }

    @PostMapping
    public ResponseEntity<TipoStockResponse> crearTipoStock(@RequestBody @Valid TipoStockRegistroRequest request) {
        log.info("Creando tipo de stock: {}", request.descripcion());
        TipoStockResponse nuevo = tipoStockServicio.crearTipoStock(request);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<TipoStockResponse>> listarTiposStock() {
        List<TipoStockResponse> tipos = tipoStockServicio.listarTiposStock();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<TipoStockResponse>> listarTiposStockActivos() {
        List<TipoStockResponse> tipos = tipoStockServicio.listarTiposStockActivos();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoStockResponse> obtenerTipoStockPorId(@PathVariable Long id) {
        TipoStockResponse tipo = tipoStockServicio.obtenerTipoStockPorId(id);
        return ResponseEntity.ok(tipo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoStockResponse> actualizarTipoStock(@PathVariable Long id,
                                                                       @RequestBody @Valid TipoStockModificacionRequest request) {
        TipoStockResponse actualizado = tipoStockServicio.actualizarTipoStock(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTipoStock(@PathVariable Long id) {
        tipoStockServicio.eliminarTipoStock(id);
        return ResponseEntity.noContent().build();
    }
}
