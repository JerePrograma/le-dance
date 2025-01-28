package ledance.servicios;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import ledance.repositorios.BonificacionRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BonificacionServicio {

    private final BonificacionRepositorio bonificacionRepositorio;

    public BonificacionServicio(BonificacionRepositorio bonificacionRepositorio) {
        this.bonificacionRepositorio = bonificacionRepositorio;
    }

    public BonificacionResponse crearBonificacion(BonificacionRequest requestDTO) {
        Bonificacion bonificacion = new Bonificacion();
        bonificacion.setDescripcion(requestDTO.descripcion());
        bonificacion.setPorcentajeDescuento(requestDTO.porcentajeDescuento());
        bonificacion.setActivo(Boolean.TRUE.equals(requestDTO.activo()));
        bonificacion.setObservaciones(requestDTO.observaciones());

        Bonificacion nuevaBonificacion = bonificacionRepositorio.save(bonificacion);
        return convertirADTO(nuevaBonificacion);
    }

    public List<BonificacionResponse> listarBonificaciones() {
        return bonificacionRepositorio.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public BonificacionResponse obtenerBonificacionPorId(Long id) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
        return convertirADTO(bonificacion);
    }

    public BonificacionResponse actualizarBonificacion(Long id, BonificacionRequest requestDTO) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));

        bonificacion.setDescripcion(requestDTO.descripcion());
        bonificacion.setPorcentajeDescuento(requestDTO.porcentajeDescuento());
        bonificacion.setActivo(Boolean.TRUE.equals(requestDTO.activo()));
        bonificacion.setObservaciones(requestDTO.observaciones());

        return convertirADTO(bonificacionRepositorio.save(bonificacion));
    }

    public void eliminarBonificacion(Long id) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
        bonificacion.setActivo(false);
        bonificacionRepositorio.save(bonificacion);
    }

    private BonificacionResponse convertirADTO(Bonificacion bonificacion) {
        return new BonificacionResponse(
                bonificacion.getId(),
                bonificacion.getDescripcion(),
                bonificacion.getPorcentajeDescuento(),
                bonificacion.getActivo(),
                bonificacion.getObservaciones()
        );
    }
}
