package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.cargo.response.CargoResponse;
import ledance.dto.stock.request.ReversionStockRequest;
import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.VentaStockRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Usuario;
import ledance.servicios.stock.StockServicio;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockControlador {
    private final StockServicio stocks;

    public StockControlador(StockServicio stocks) {
        this.stocks = stocks;
    }

    @PostMapping
    public StockResponse crear(@Valid @RequestBody StockRegistroRequest request,
                               @AuthenticationPrincipal Usuario usuario) {
        return stocks.crearStock(request, usuario);
    }

    @GetMapping
    public List<StockResponse> listar() {
        return stocks.listarStocks();
    }

    @GetMapping("/activos")
    public List<StockResponse> listarActivos() {
        return stocks.listarStocksActivos();
    }

    @GetMapping("/{id}")
    public StockResponse obtener(@PathVariable Long id) {
        return stocks.obtenerStockPorId(id);
    }

    @PutMapping("/{id}")
    public StockResponse actualizar(@PathVariable Long id, @Valid @RequestBody StockRegistroRequest request) {
        return stocks.actualizarStock(id, request);
    }

    @DeleteMapping("/{id}")
    public void darBaja(@PathVariable Long id) {
        stocks.eliminarStock(id);
    }

    @PostMapping("/ventas")
    public CargoResponse vender(@Valid @RequestBody VentaStockRequest request,
                                @AuthenticationPrincipal Usuario usuario) {
        return stocks.vender(request, usuario);
    }

    @PostMapping("/ventas/{id}/reversion")
    public CargoResponse revertir(@PathVariable Long id,
                                  @Valid @RequestBody ReversionStockRequest request,
                                  @AuthenticationPrincipal Usuario usuario) {
        return stocks.revertirVenta(id, request, usuario);
    }
}
