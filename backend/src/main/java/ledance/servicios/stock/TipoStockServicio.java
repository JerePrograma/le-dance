package ledance.servicios.stock;

import ledance.dto.stock.request.TipoStockRegistroRequest;
import ledance.dto.stock.request.TipoStockModificacionRequest;
import ledance.dto.stock.response.TipoStockResponse;
import ledance.dto.stock.TipoStockMapper;
import ledance.entidades.TipoStock;
import ledance.repositorios.TipoStockRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TipoStockServicio {

    private final TipoStockRepositorio tipoStockRepositorio;
    private final TipoStockMapper tipoStockMapper;

    public TipoStockServicio(TipoStockRepositorio tipoStockRepositorio,
                                TipoStockMapper tipoStockMapper) {
        this.tipoStockRepositorio = tipoStockRepositorio;
        this.tipoStockMapper = tipoStockMapper;
    }

    @Transactional
    public TipoStockResponse crearTipoStock(TipoStockRegistroRequest request) {
        TipoStock tipoStock = tipoStockMapper.toEntity(request);
        TipoStock saved = tipoStockRepositorio.save(tipoStock);
        return tipoStockMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<TipoStockResponse> listarTiposStock() {
        return tipoStockRepositorio.findAll().stream()
                .map(tipoStockMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TipoStockResponse> listarTiposStockActivos() {
        return tipoStockRepositorio.findByActivoTrue().stream()
                .map(tipoStockMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoStockResponse obtenerTipoStockPorId(Long id) {
        TipoStock tipoStock = tipoStockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + id));
        return tipoStockMapper.toDTO(tipoStock);
    }

    @Transactional
    public TipoStockResponse actualizarTipoStock(Long id, TipoStockModificacionRequest request) {
        TipoStock tipoStock = tipoStockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + id));
        tipoStockMapper.updateEntityFromRequest(request, tipoStock);
        TipoStock updated = tipoStockRepositorio.save(tipoStock);
        return tipoStockMapper.toDTO(updated);
    }

    @Transactional
    public void eliminarTipoStock(Long id) {
        TipoStock tipoStock = tipoStockRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TipoStock no encontrado con id: " + id));
        tipoStock.setActivo(false);
        tipoStockRepositorio.save(tipoStock);
    }
}
