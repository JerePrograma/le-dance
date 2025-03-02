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
        Stock stock = stockMapper.toEntity(request);
        stock.setTipo(
                tipoStockRepositorio.findById(request.tipoStockId())
                        .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + request.tipoStockId()))
        );
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
        // Asigna el TipoStock a partir del id (ahora mapeado como tipoId en el request)
        stock.setTipo(
                tipoStockRepositorio.findById(request.tipoId())
                        .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + request.tipoId()))
        );
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

    @Transactional(readOnly = true)
    public List<StockResponse> listarStocksConceptos() {
        return stockRepositorio.findByTipoDescripcion("Concepto").stream()
                .map(stockMapper::toDTO)
                .collect(Collectors.toList());
    }
}
