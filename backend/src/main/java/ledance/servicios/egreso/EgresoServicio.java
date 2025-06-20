package ledance.servicios.egreso;

import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.egreso.EgresoMapper;
import ledance.entidades.Egreso;
import ledance.entidades.MetodoPago;
import ledance.repositorios.EgresoRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EgresoServicio {

    private final EgresoRepositorio egresoRepositorio;
    private final EgresoMapper egresoMapper;
    private final MetodoPagoRepositorio metodoPagoRepositorio;

    public EgresoServicio(EgresoRepositorio egresoRepositorio,
                          EgresoMapper egresoMapper,
                          MetodoPagoRepositorio metodoPagoRepositorio) {
        this.egresoRepositorio = egresoRepositorio;
        this.egresoMapper = egresoMapper;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
    }

    @Transactional
    public EgresoResponse agregarEgreso(EgresoRegistroRequest request) {
        // Mapear el request a la entidad
        Egreso egreso = egresoMapper.toEntity(request);

        MetodoPago metodo = metodoPagoRepositorio
                .findByDescripcionContainingIgnoreCase(request.metodoPagoDescripcion());
        if (metodo != null) {
            egreso.setMetodoPago(metodo);
            // Aseguramos que el egreso este activo
            egreso.setActivo(true);
            Egreso saved = egresoRepositorio.save(egreso);
            return egresoMapper.toDTO(saved);
        }
        return null;
    }

    @Transactional
    public EgresoResponse actualizarEgreso(Long id, EgresoRegistroRequest request) {
        Egreso egreso = egresoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado para id: " + id));
        // Actualizar campos mediante el mapper
        egresoMapper.updateEntityFromRequest(request, egreso);
        // Actualizar el metodo de pago si se envia en el request
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado para id: " + request.metodoPagoId()));
            egreso.setMetodoPago(metodo);
        }
        Egreso saved = egresoRepositorio.save(egreso);
        return egresoMapper.toDTO(saved);
    }

    @Transactional
    public void eliminarEgreso(Long id) {
        Egreso egreso = egresoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado para id: " + id));
        // Realizamos una baja logica (marcandolo inactivo)
        egreso.setActivo(false);
        egresoRepositorio.save(egreso);
    }

    public EgresoResponse obtenerEgresoPorId(Long id) {
        Egreso egreso = egresoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado para id: " + id));
        return egresoMapper.toDTO(egreso);
    }

    public List<EgresoResponse> listarEgresos() {
        return egresoRepositorio.findAll()
                .stream()
                .filter(eg -> eg.getActivo() != null && eg.getActivo())
                .map(egresoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<EgresoResponse> listarEgresosPorMetodo(String metodoDescripcion) {
        return egresoRepositorio.findAll()
                .stream()
                .filter(eg -> eg.getActivo() != null && eg.getActivo()
                        && eg.getMetodoPago() != null
                        && eg.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .map(egresoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<EgresoResponse> listarEgresosDebito() {
        return listarEgresosPorMetodo("DEBITO");
    }

    public List<EgresoResponse> listarEgresosEfectivo() {
        return listarEgresosPorMetodo("EFECTIVO");
    }

}
