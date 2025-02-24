package ledance.servicios.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.dto.stock.StockMapper;
import ledance.entidades.Stock;
import ledance.repositorios.StockRepositorio;
import ledance.repositorios.TipoStockRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockServicio {

    private final StockRepositorio stockRepositorio;
    private final TipoStockRepositorio tipoStockRepositorio;
    private final StockMapper stockMapper;

    public StockServicio(StockRepositorio stockRepositorio,
                            TipoStockRepositorio tipoStockRepositorio,
                            StockMapper stockMapper) {
        this.stockRepositorio = stockRepositorio;
        this.tipoStockRepositorio = tipoStockRepositorio;
        this.stockMapper = stockMapper;
    }

    @Transactional
    public StockResponse crearStock(StockRegistroRequest request) {
        // Convertir el request a entidad
        Stock stock = stockMapper.toEntity(request);
        // Asignar el TipoStock usando el id del request
        stock.setTipo(tipoStockRepositorio.findById(request.tipoStockId())
                .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + request.tipoStockId())));
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
        // Actualizar usando el mapper
        stockMapper.updateEntityFromRequest(request, stock);
        // Asignar el TipoStock desde el request
        stock.setTipo(tipoStockRepositorio.findById(request.tipoId())
                .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + request.tipoId())));
        Stock updated = stockRepositorio.save(stock);
        return stockMapper.toDTO(updated);
    }

    @Transactional
    public void eliminarStock(Long id) {
        Stock stock = stockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado con id: " + id));
        // Baja lógica: marcar como inactivo
        stock.setActivo(false);
        stockRepositorio.save(stock);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> listarStocksConceptos() {
        return stockRepositorio.findByTipoDescripcion("Concepto")
                .stream()
                .map(stockMapper::toDTO)
                .collect(Collectors.toList());
    }

}
