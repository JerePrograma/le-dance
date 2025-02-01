package ledance.servicios;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import ledance.dto.mappers.BonificacionMapper;
import ledance.repositorios.BonificacionRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BonificacionServicio implements IBonificacionServicio {

    private static final Logger log = LoggerFactory.getLogger(BonificacionServicio.class);

    private final BonificacionRepositorio bonificacionRepositorio;
    private final BonificacionMapper bonificacionMapper;

    public BonificacionServicio(BonificacionRepositorio bonificacionRepositorio, BonificacionMapper bonificacionMapper) {
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.bonificacionMapper = bonificacionMapper;
    }

    @Override
    @Transactional
    public BonificacionResponse crearBonificacion(BonificacionRequest requestDTO) {
        log.info("Creando bonificación: {}", requestDTO.descripcion());
        Bonificacion bonificacion = bonificacionMapper.toEntity(requestDTO);
        Bonificacion guardada = bonificacionRepositorio.save(bonificacion);
        return bonificacionMapper.toDTO(guardada);
    }

    @Override
    public List<BonificacionResponse> listarBonificaciones() {
        return bonificacionRepositorio.findAll().stream()
                .map(bonificacionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BonificacionResponse obtenerBonificacionPorId(Long id) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada."));
        return bonificacionMapper.toDTO(bonificacion);
    }

    @Override
    @Transactional
    public BonificacionResponse actualizarBonificacion(Long id, BonificacionRequest requestDTO) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada."));
        // Actualizar campos manualmente
        bonificacion.setDescripcion(requestDTO.descripcion());
        bonificacion.setPorcentajeDescuento(requestDTO.porcentajeDescuento());
        bonificacion.setActivo(Boolean.TRUE.equals(requestDTO.activo()));
        bonificacion.setObservaciones(requestDTO.observaciones());
        Bonificacion actualizada = bonificacionRepositorio.save(bonificacion);
        return bonificacionMapper.toDTO(actualizada);
    }

    @Override
    @Transactional
    public void eliminarBonificacion(Long id) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada."));
        bonificacion.setActivo(false);
        bonificacionRepositorio.save(bonificacion);
    }
}
