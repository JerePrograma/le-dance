package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@Validated
public class StockControlador {

    private static final Logger log = LoggerFactory.getLogger(StockControlador.class);
    private final StockServicio stockServicio;

    public StockControlador(StockServicio stockServicio) {
        this.stockServicio = stockServicio;
    }

    @PostMapping
    public ResponseEntity<StockResponse> crearStock(@RequestBody @Valid StockRegistroRequest request) {
        StockResponse nuevo = stockServicio.crearStock(request);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> listarStocks() {
        List<StockResponse> stocks = stockServicio.listarStocks();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<StockResponse>> listarStocksActivos() {
        List<StockResponse> stocks = stockServicio.listarStocksActivos();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> obtenerStockPorId(@PathVariable Long id) {
        StockResponse stock = stockServicio.obtenerStockPorId(id);
        return ResponseEntity.ok(stock);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockResponse> actualizarStock(@PathVariable Long id,
                                                         @RequestBody @Valid StockModificacionRequest request) {
        StockResponse actualizado = stockServicio.actualizarStock(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarStock(@PathVariable Long id) {
        stockServicio.eliminarStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conceptos")
    public ResponseEntity<List<StockResponse>> listarStocksConceptos() {
        List<StockResponse> stocks = stockServicio.listarStocksConceptos();
        return ResponseEntity.ok(stocks);
    }
}
