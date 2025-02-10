package ledance.servicios;

import ledance.dto.mappers.BonificacionMapper;
import ledance.dto.request.BonificacionModificacionRequest;
import ledance.dto.request.BonificacionRegistroRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
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

    /**
     * ✅ Crear una nueva bonificacion
     */
    @Override
    @Transactional
    public BonificacionResponse crearBonificacion(BonificacionRegistroRequest requestDTO) {
        log.info("Creando bonificacion: {}", requestDTO.descripcion());

        // Validacion para evitar duplicados
        if (bonificacionRepositorio.existsByDescripcion(requestDTO.descripcion())) {
            throw new IllegalArgumentException("Ya existe una bonificacion con la misma descripcion.");
        }

        Bonificacion bonificacion = bonificacionMapper.toEntity(requestDTO);
        Bonificacion guardada = bonificacionRepositorio.save(bonificacion);
        return bonificacionMapper.toDTO(guardada);
    }

    /**
     * ✅ Listar todas las bonificaciones activas
     */
    @Override
    public List<BonificacionResponse> listarBonificaciones() {
        return bonificacionRepositorio.findByActivoTrue().stream()
                .map(bonificacionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Obtener una bonificacion por ID
     */
    @Override
    public BonificacionResponse obtenerBonificacionPorId(Long id) {
        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
        return bonificacionMapper.toDTO(bonificacion);
    }

    /**
     * ✅ Actualizar una bonificacion existente
     */
    @Override
    @Transactional
    public BonificacionResponse actualizarBonificacion(Long id, BonificacionModificacionRequest requestDTO) {
        log.info("Actualizando bonificacion con id: {}", id);

        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));

        // Uso del mapper para actualizar la entidad
        bonificacionMapper.updateEntityFromRequest(requestDTO, bonificacion);

        Bonificacion actualizada = bonificacionRepositorio.save(bonificacion);
        return bonificacionMapper.toDTO(actualizada);
    }

    /**
     * ✅ Baja logica de una bonificacion (desactivar)
     */
    @Override
    @Transactional
    public void eliminarBonificacion(Long id) {
        log.info("Dando de baja la bonificacion con id: {}", id);

        Bonificacion bonificacion = bonificacionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));

        if (!bonificacion.getActivo()) {
            throw new IllegalStateException("La bonificacion ya esta inactiva.");
        }

        bonificacion.setActivo(false);
        bonificacionRepositorio.save(bonificacion);
    }
}
