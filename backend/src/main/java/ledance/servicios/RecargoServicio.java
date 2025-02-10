package ledance.servicios;

import ledance.dto.mappers.RecargoMapper;
import ledance.dto.request.RecargoRegistroRequest;
import ledance.dto.response.RecargoResponse;
import ledance.entidades.Recargo;
import ledance.repositorios.RecargoDetalleRepositorio;
import ledance.repositorios.RecargoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RecargoServicio {

    private final RecargoRepositorio recargoRepositorio;
    private final RecargoDetalleRepositorio recargoDetalleRepositorio;
    private final RecargoMapper recargoMapper;

    public RecargoServicio(
            RecargoRepositorio recargoRepositorio,
            RecargoDetalleRepositorio recargoDetalleRepositorio,
            RecargoMapper recargoMapper
    ) {
        this.recargoRepositorio = recargoRepositorio;
        this.recargoDetalleRepositorio = recargoDetalleRepositorio;
        this.recargoMapper = recargoMapper;
    }

    /**
     * Crear un nuevo recargo con sus detalles (usando el mapper).
     */
    public RecargoResponse crearRecargo(RecargoRegistroRequest request) {
        // 1. Convertimos el DTO de request a la entidad Recargo (con sus RecargoDetalle).
        //    Esto asigna la lista de detalles y les coloca el 'recargo' padre.
        Recargo recargo = recargoMapper.toEntity(request);

        // 2. Guardamos en BD
        //    (al tener cascade/orphanRemoval puede autoguardar detalles, o
        //     si no se hace, llamamos a saveAll(...) para los detalles).
        recargoRepositorio.save(recargo);

        // 3. Convertimos la entidad guardada a DTO de respuesta
        return recargoMapper.toResponse(recargo);
    }

    /**
     * Listar todos los recargos, retornando sus DTOs.
     */
    @Transactional(readOnly = true)
    public List<RecargoResponse> listarRecargos() {
        List<Recargo> recargos = recargoRepositorio.findAll();
        return recargos
                .stream()
                .map(recargoMapper::toResponse)
                .toList();
    }

    /**
     * Obtener un recargo por ID, como DTO de respuesta.
     */
    @Transactional(readOnly = true)
    public RecargoResponse obtenerRecargo(Long id) {
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));
        return recargoMapper.toResponse(recargo);
    }

    /**
     * Actualizar la descripcion y (opcionalmente) los detalles de un recargo.
     * Este ejemplo asume que seguimos usando RecargoRegistroRequest
     * como “modificacion” tambien. Podrias separar en "RecargoModificacionRequest".
     */
    public RecargoResponse actualizarRecargo(Long id, RecargoRegistroRequest request) {
        // 1. Buscamos el recargo existente
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));

        // 2. Actualizamos la descripcion
        recargo.setDescripcion(request.descripcion());

        // 3. Eliminamos los detalles previos (opcional, segun tu estrategia)
        recargoDetalleRepositorio.deleteAllByRecargoId(recargo.getId());

        // 4. Mapear nuevamente la lista de detalles del request
        //    y asignarlos al recargo actual
        //    (evitando crear un recargo nuevo).
        var nuevosDetalles = recargoMapper.toDetalleEntityList(request.detalles());
        nuevosDetalles.forEach(det -> det.setRecargo(recargo));

        // 5. Guardar detalles
        recargoDetalleRepositorio.saveAll(nuevosDetalles);

        recargo.setDetalles(nuevosDetalles);

        // 6. Retornamos la entidad a la BD
        recargoRepositorio.save(recargo);

        // 7. Convertimos a DTO
        return recargoMapper.toResponse(recargo);
    }

    /**
     * Eliminar un recargo (y sus detalles).
     */
    public void eliminarRecargo(Long id) {
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));
        recargoRepositorio.delete(recargo);
    }

    public void obtenerRecargoPorId(Long recargoId) {
        recargoRepositorio.findById(recargoId)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + recargoId));
    }

}
