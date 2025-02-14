package ledance.servicios.caja;

import ledance.dto.caja.CajaMapper;
import ledance.dto.caja.request.CajaRegistroRequest;
import ledance.dto.caja.request.CajaModificacionRequest;
import ledance.dto.caja.response.CajaResponse;
import ledance.entidades.Caja;
import ledance.repositorios.CajaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class CajaServicio {

    private final CajaRepositorio cajaRepositorio;
    private final CajaMapper cajaMapper;

    public CajaServicio(CajaRepositorio cajaRepositorio, CajaMapper cajaMapper) {
        this.cajaRepositorio = cajaRepositorio;
        this.cajaMapper = cajaMapper;
    }

    public CajaResponse registrarCaja(CajaRegistroRequest request) {
        Caja caja = cajaMapper.toEntity(request);
        Caja guardado = cajaRepositorio.save(caja);
        return cajaMapper.toDTO(guardado);
    }

    public CajaResponse actualizarCaja(Long id, CajaModificacionRequest request) {
        Caja caja = cajaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada con id: " + id));
        cajaMapper.updateEntityFromRequest(request, caja);
        Caja actualizado = cajaRepositorio.save(caja);
        return cajaMapper.toDTO(actualizado);
    }

    @Transactional(readOnly = true)
    public Page<CajaResponse> listarCajas(Pageable pageable) {
        return cajaRepositorio.findAll(pageable)
                .map(cajaMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaPorId(Long id) {
        Caja caja = cajaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada con id: " + id));
        return cajaMapper.toDTO(caja);
    }

    // Método adicional para listar ingresos diarios por fecha
    @Transactional(readOnly = true)
    public Page<CajaResponse> listarCajasPorFecha(LocalDate fecha, Pageable pageable) {
        return cajaRepositorio.findByFecha(fecha, pageable)
                .map(cajaMapper::toDTO);
    }
}
