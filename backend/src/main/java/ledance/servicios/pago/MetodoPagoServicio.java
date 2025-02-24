// src/main/java/ledance/servicios/pago/MetodoPagoServicio.java
package ledance.servicios.pago;

import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.metodopago.request.MetodoPagoModificacionRequest;
import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.response.MetodoPagoResponse;
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
     * Crea un nuevo método de pago.
     */
    public MetodoPagoResponse registrar(MetodoPagoRegistroRequest request) {
        MetodoPago nuevo = metodoPagoMapper.toEntity(request);
        MetodoPago guardado = metodoPagoRepositorio.save(nuevo);
        return metodoPagoMapper.toDTO(guardado);
    }

    /**
     * Lista todos los métodos de pago.
     */
    @Transactional(readOnly = true)
    public List<MetodoPagoResponse> listar() {
        return metodoPagoRepositorio.findAll().stream()
                .map(metodoPagoMapper::toDTO)
                .toList();
    }

    /**
     * Obtiene un método de pago por su ID.
     */
    @Transactional(readOnly = true)
    public MetodoPagoResponse obtenerPorId(Long id) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el método de pago con ID: " + id));
        return metodoPagoMapper.toDTO(metodo);
    }

    /**
     * Actualiza un método de pago existente.
     */
    public MetodoPagoResponse actualizar(Long id, MetodoPagoModificacionRequest request) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el método de pago con ID: " + id));
        metodoPagoMapper.updateEntityFromRequest(request, metodo);
        MetodoPago actualizado = metodoPagoRepositorio.save(metodo);
        return metodoPagoMapper.toDTO(actualizado);
    }

    /**
     * Realiza la baja lógica de un método de pago.
     */
    public void eliminar(Long id) {
        MetodoPago metodo = metodoPagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el método de pago con ID: " + id));
        metodo.setActivo(false);
        metodoPagoRepositorio.save(metodo);
    }
}
