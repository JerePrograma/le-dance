package ledance.servicios;

import ledance.dto.mappers.MetodoPagoMapper;
import ledance.dto.request.MetodoPagoModificacionRequest;
import ledance.dto.request.MetodoPagoRegistroRequest;
import ledance.dto.response.MetodoPagoResponse;
import ledance.entidades.MetodoPago;
import ledance.repositorios.MetodoPagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MetodoPagoServicio {

    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final MetodoPagoMapper metodoPagoMapper;

    public MetodoPagoServicio(MetodoPagoRepositorio metodoPagoRepositorio,
                              MetodoPagoMapper metodoPagoMapper) {
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.metodoPagoMapper = metodoPagoMapper;
    }

    /**
     * Crear (registrar) un nuevo metodo de pago.
     */
    public MetodoPagoResponse registrar(MetodoPagoRegistroRequest request) {
        // Convertimos DTO a entidad
        MetodoPago nuevo = metodoPagoMapper.toEntity(request);
        // Guardamos en la BD
        MetodoPago guardado = metodoPagoRepositorio.save(nuevo);
        // Retornamos la respuesta
        return metodoPagoMapper.toDTO(guardado);
    }

    /**
     * Listar todos los metodos de pago (puedes filtrar solo activos si gustas).
     */
    @Transactional(readOnly = true)
    public List<MetodoPagoResponse> listar() {
        // Si deseas solo los activos, puedes usar un custom query: findByActivoTrue()
        return metodoPagoRepositorio.findAll().stream()
                .map(metodoPagoMapper::toDTO)
                .toList();
    }

    /**
     * Obtener un metodo de pago por ID.
     */
    @Transactional(readOnly = true)
    public MetodoPagoResponse obtenerPorId(Long id) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el metodo de pago con ID: " + id));
        return metodoPagoMapper.toDTO(metodo);
    }

    /**
     * Actualizar un metodo de pago existente.
     */
    public MetodoPagoResponse actualizar(Long id, MetodoPagoModificacionRequest request) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el metodo de pago con ID: " + id));
        // MapStruct actualiza campos en la entidad
        metodoPagoMapper.updateEntityFromRequest(request, metodo);
        // Guardamos cambios
        MetodoPago actualizado = metodoPagoRepositorio.save(metodo);
        return metodoPagoMapper.toDTO(actualizado);
    }

    /**
     * Baja logica: setear activo=false.
     */
    public void eliminar(Long id) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el metodo de pago con ID: " + id));
        metodo.setActivo(false);
        metodoPagoRepositorio.save(metodo);
    }
}
