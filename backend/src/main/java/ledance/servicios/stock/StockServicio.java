package ledance.servicios.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.dto.stock.StockMapper;
import ledance.entidades.Stock;
import ledance.repositorios.StockRepositorio;
import ledance.servicios.pago.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockServicio {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

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

    @Transactional(readOnly = true)
    public StockResponse obtenerStockPorNombre(String nombre) {
        String nombreNormalizado = nombre.trim();
        log.debug("[obtenerStockPorNombre] Valor recibido: '{}', normalizado a: '{}'", nombre, nombreNormalizado);
        Optional<Stock> optionalStock = stockRepositorio.findByNombreIgnoreCase(nombreNormalizado);
        if (optionalStock.isPresent()) {
            Stock stock = optionalStock.get();
            log.debug("[obtenerStockPorNombre] Stock encontrado: id={}, nombre='{}', stock={}",
                    stock.getId(), stock.getNombre(), stock.getStock());
            return stockMapper.toDTO(stock);
        } else {
            log.warn("[obtenerStockPorNombre] No se encontró stock con nombre: '{}'", nombreNormalizado);
            throw new IllegalArgumentException("Stock no encontrado con nombre: " + nombreNormalizado);
        }
    }

    @Transactional
    public void reducirStock(String nombre, int cantidad) {
        String nombreNormalizado = nombre.trim();
        log.debug("[reducirStock] Valor recibido: '{}', normalizado a: '{}'", nombre, nombreNormalizado);
        Stock stock = stockRepositorio.findByNombreIgnoreCase(nombreNormalizado)
                .orElseThrow(() -> {
                    log.error("[reducirStock] No se encontró stock con nombre: '{}'", nombreNormalizado);
                    return new IllegalArgumentException("No se encontró stock con nombre: " + nombreNormalizado);
                });
        log.debug("[reducirStock] Stock actual: {} unidades para '{}'", stock.getStock(), stock.getNombre());
        if (stock.getStock() < cantidad) {
            log.error("[reducirStock] No hay suficiente stock para '{}'. Cantidad requerida: {}, disponible: {}",
                    nombreNormalizado, cantidad, stock.getStock());
            throw new IllegalArgumentException("No hay suficiente stock para el producto: " + nombreNormalizado);
        }
        stock.setStock(stock.getStock() - cantidad);
        stockRepositorio.save(stock);
        log.debug("[reducirStock] Stock actualizado para '{}'. Nueva cantidad: {}", nombreNormalizado, stock.getStock());
    }


}
