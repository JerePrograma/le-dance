package ledance.servicios.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.dto.stock.StockMapper;
import ledance.entidades.Stock;
import ledance.repositorios.StockRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockServicio {

    private final StockRepositorio stockRepositorio;
    private final StockMapper stockMapper;

    public StockServicio(StockRepositorio stockRepositorio,
                         StockMapper stockMapper) {
        this.stockRepositorio = stockRepositorio;
        this.stockMapper = stockMapper;
    }

    @Transactional
    public StockResponse crearStock(StockRegistroRequest request) {
        Stock stock = stockMapper.toEntity(request);

        stock.setFechaIngreso(request.fechaIngreso());
        stock.setFechaEgreso(request.fechaEgreso());
        Stock saved = stockRepositorio.save(stock);
        return stockMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> listarStocks() {
        return stockRepositorio.findAll().stream()
                .map(stockMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockResponse> listarStocksActivos() {
        return stockRepositorio.findByActivoTrue().stream()
                .map(stockMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockResponse obtenerStockPorId(Long id) {
        Stock stock = stockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado con id: " + id));
        return stockMapper.toDTO(stock);
    }

    @Transactional
    public StockResponse actualizarStock(Long id, StockModificacionRequest request) {
        Stock stock = stockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado con id: " + id));
        // Actualiza la entidad usando el mapper, que convierte el tipoEgreso y mapea el id del tipo
        stockMapper.updateEntityFromRequest(request, stock);

        stock.setFechaIngreso(request.fechaIngreso());
        stock.setFechaEgreso(request.fechaEgreso());
        Stock updated = stockRepositorio.save(stock);
        return stockMapper.toDTO(updated);
    }

    @Transactional
    public void eliminarStock(Long id) {
        Stock stock = stockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado con id: " + id));
        // Baja logica: marcar como inactivo
        stock.setActivo(false);
        stockRepositorio.save(stock);
    }

}
