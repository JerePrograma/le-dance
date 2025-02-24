package ledance.servicios.concepto;

import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.request.SubConceptoModificacionRequest;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.dto.concepto.SubConceptoMapper;
import ledance.entidades.SubConcepto;
import ledance.repositorios.SubConceptoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubConceptoServicio {

    private final SubConceptoRepositorio subConceptoRepositorio;
    private final SubConceptoMapper subConceptoMapper;

    public SubConceptoServicio(SubConceptoRepositorio subConceptoRepositorio,
                               SubConceptoMapper subConceptoMapper) {
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.subConceptoMapper = subConceptoMapper;
    }

    @Transactional
    public SubConceptoResponse crearSubConcepto(SubConceptoRegistroRequest request) {
        // Forzamos la descripción a mayúsculas
        SubConceptoRegistroRequest modRequest = new SubConceptoRegistroRequest(request.descripcion().toUpperCase());
        SubConcepto subConcepto = subConceptoMapper.toEntity(modRequest);
        SubConcepto saved = subConceptoRepositorio.save(subConcepto);
        return subConceptoMapper.toResponse(saved);
    }

    @Transactional
    public SubConceptoResponse actualizarSubConcepto(Long id, SubConceptoModificacionRequest request) {
        SubConcepto subConcepto = subConceptoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SubConcepto no encontrado con id: " + id));
        // Actualiza manualmente el campo transformándolo a mayúsculas
        subConcepto.setDescripcion(request.descripcion().toUpperCase());
        SubConcepto updated = subConceptoRepositorio.save(subConcepto);
        return subConceptoMapper.toResponse(updated);
    }


    @Transactional(readOnly = true)
    public List<SubConceptoResponse> listarSubConceptos() {
        return subConceptoRepositorio.findAll().stream()
                .map(subConceptoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubConceptoResponse obtenerSubConceptoPorId(Long id) {
        SubConcepto subConcepto = subConceptoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SubConcepto no encontrado con id: " + id));
        return subConceptoMapper.toResponse(subConcepto);
    }

    @Transactional
    public void eliminarSubConcepto(Long id) {
        if (!subConceptoRepositorio.existsById(id)) {
            throw new IllegalArgumentException("SubConcepto no encontrado con id: " + id);
        }
        subConceptoRepositorio.deleteById(id);
    }
}
