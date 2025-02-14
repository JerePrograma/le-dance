package ledance.servicios.recargo;

import ledance.dto.recargodetalle.RecargoMapper;
import ledance.dto.recargodetalle.request.RecargoDetalleModificacionRegistroRequest;
import ledance.dto.recargodetalle.request.RecargoDetalleRegistroRequest;
import ledance.dto.recargodetalle.response.RecargoDetalleResponse;
import ledance.entidades.RecargoDetalle;
import ledance.repositorios.RecargoDetalleRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RecargoDetalleServicio {

    private final RecargoDetalleRepositorio recargoDetalleRepositorio;
    private final RecargoServicio recargoServicio;
    private final RecargoMapper recargoMapper;

    public RecargoDetalleServicio(
            RecargoDetalleRepositorio recargoDetalleRepositorio,
            RecargoServicio recargoServicio,
            RecargoMapper recargoMapper
    ) {
        this.recargoDetalleRepositorio = recargoDetalleRepositorio;
        this.recargoServicio = recargoServicio;
        this.recargoMapper = recargoMapper;
    }

    /**
     * Agregar un nuevo detalle a un recargo existente.
     * Retorna DTO de respuesta.
     */
    public RecargoDetalleResponse agregarDetalle(Long recargoId, RecargoDetalleRegistroRequest request) {
        // 1. Verificamos que el Recargo existe (lanza excepcion si no)
        recargoServicio.obtenerRecargoPorId(recargoId);
        // Esto devuelve la entidad, pero si solo quieres verificar su existencia
        // puedes ignorar la variable o re-mapperla.

        // 2. Mapear Request -> Entidad
        RecargoDetalle detalle = recargoMapper.toEntity(request);

        // 3. Asignar la FK al recargo “padre”
        detalle.getRecargo().setId(recargoId);
        // O bien, si 'toEntity(request)' no setea Recargo por default,
        // tendras que inyectarlo manualmente.
        // Ejs:
        // Recargo recargo = recargoServicio.obtenerRecargoPorId(recargoId);
        // detalle.setRecargo(recargo);

        // 4. Guardar
        recargoDetalleRepositorio.save(detalle);

        // 5. Retornar la version convertida a Response
        return recargoMapper.toResponse(detalle);
    }

    /**
     * Listar los detalles de un recargo concreto, devolviendo DTOs de respuesta.
     */
    @Transactional(readOnly = true)
    public List<RecargoDetalleResponse> listarDetallesPorRecargo(Long recargoId) {
        var lista = recargoDetalleRepositorio.findAllByRecargoId(recargoId);
        return lista.stream()
                .map(recargoMapper::toResponse)
                .toList();
    }

    /**
     * Actualizar un detalle de recargo puntual (por ID).
     */
    public RecargoDetalleResponse actualizarDetalle(Long detalleId, RecargoDetalleModificacionRegistroRequest request) {
        // 1. Buscar entidad en BD
        RecargoDetalle detalle = recargoDetalleRepositorio.findById(detalleId)
                .orElseThrow(() -> new IllegalArgumentException("RecargoDetalle no encontrado con id: " + detalleId));

        // 2. Actualizar
        detalle.setDiaDesde(request.diaDesde());
        detalle.setPorcentaje(request.porcentaje());

        recargoDetalleRepositorio.save(detalle);

        // 3. Convertir a DTO y retornar
        return recargoMapper.toResponse(detalle);
    }

    /**
     * Eliminar un detalle individual.
     */
    public void eliminarDetalle(Long detalleId) {
        RecargoDetalle detalle = recargoDetalleRepositorio.findById(detalleId)
                .orElseThrow(() -> new IllegalArgumentException("RecargoDetalle no encontrado con id: " + detalleId));

        recargoDetalleRepositorio.delete(detalle);
    }
}