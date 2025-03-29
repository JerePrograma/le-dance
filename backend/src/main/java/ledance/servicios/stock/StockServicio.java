package ledance.servicios.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.dto.stock.StockMapper;
import ledance.entidades.Stock;
import ledance.infra.errores.SinStockException;
import ledance.repositorios.StockRepositorio;
import ledance.servicios.pago.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        stock.setNombre(stock.getNombre().toUpperCase());
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
    public StockResponse actualizarStock(Long id, StockRegistroRequest request) {
        Stock stock = stockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado con id: " + id));
        // Actualiza la entidad usando el mapper, que convierte el tipoEgreso y mapea el id del tipo
        stockMapper.updateEntityFromRequest(request, stock);

        stock.setNombre(stock.getNombre().toUpperCase());
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
    public boolean obtenerStockPorNombre(String nombre) {
        String nombreNormalizado = nombre.trim();
        log.info("[obtenerStockPorNombre] Valor recibido: '{}', normalizado a: '{}'", nombre, nombreNormalizado);
        boolean existe = stockRepositorio.findByNombreIgnoreCase(nombreNormalizado).isPresent();
        log.info("[obtenerStockPorNombre] Resultado: {}", existe);
        return existe;
    }

    @Transactional
    public void reducirStock(String nombre, int cantidad) {
        String nombreNormalizado = nombre.trim();
        log.info("[reducirStock] Valor recibido: '{}', normalizado a: '{}'", nombre, nombreNormalizado);

        Stock stock = stockRepositorio.findByNombreIgnoreCase(nombreNormalizado)
                .orElseThrow(() -> {
                    log.error("[reducirStock] No se encontro stock con nombre: '{}'", nombreNormalizado);
                    return new SinStockException("No se encontro stock con nombre: " + nombreNormalizado);
                });

        log.info("[reducirStock] Stock actual para '{}': {} unidades", nombreNormalizado, stock.getStock());

        if (stock.getStock() < cantidad) {
            log.error("[reducirStock] No hay suficiente stock para '{}'. Requerido: {}, disponible: {}",
                    nombreNormalizado, cantidad, stock.getStock());
            throw new SinStockException("No hay suficiente stock para el producto: " + nombreNormalizado +
                    ". Requerido: " + cantidad + ", disponible: " + stock.getStock());
        }

        stock.setStock(stock.getStock() - cantidad);
        stockRepositorio.save(stock);
        log.info("[reducirStock] Stock actualizado para '{}'. Nueva cantidad: {}", nombreNormalizado, stock.getStock());
    }

}
