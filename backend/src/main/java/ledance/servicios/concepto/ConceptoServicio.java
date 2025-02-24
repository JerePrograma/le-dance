package ledance.servicios.concepto;

import ledance.dto.concepto.request.ConceptoRegistroRequest;
import ledance.dto.concepto.request.ConceptoModificacionRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.concepto.ConceptoMapper;
import ledance.entidades.Concepto;
import ledance.entidades.SubConcepto;
import ledance.repositorios.ConceptoRepositorio;
import ledance.repositorios.SubConceptoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConceptoServicio {

    private final ConceptoRepositorio conceptoRepositorio;
    private final SubConceptoRepositorio subConceptoRepositorio;
    private final ConceptoMapper conceptoMapper;

    public ConceptoServicio(ConceptoRepositorio conceptoRepositorio,
                            SubConceptoRepositorio subConceptoRepositorio,
                            ConceptoMapper conceptoMapper) {
        this.conceptoRepositorio = conceptoRepositorio;
        this.subConceptoRepositorio = subConceptoRepositorio;
        this.conceptoMapper = conceptoMapper;
    }

    @Transactional
    public ConceptoResponse crearConcepto(ConceptoRegistroRequest request) {
        Concepto concepto = conceptoMapper.toEntity(request);
        SubConcepto subConcepto = subConceptoRepositorio.findById(request.subConceptoId())
                .orElseThrow(() -> new IllegalArgumentException("SubConcepto no encontrado con id: " + request.subConceptoId()));
        // Forzamos la descripción a mayúsculas
        subConcepto.setDescripcion(subConcepto.getDescripcion().toUpperCase());
        concepto.setSubConcepto(subConcepto);
        Concepto saved = conceptoRepositorio.save(concepto);
        return conceptoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConceptoResponse> listarConceptos() {
        return conceptoRepositorio.findAll().stream()
                .map(conceptoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConceptoResponse obtenerConceptoPorId(Long id) {
        Concepto concepto = conceptoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concepto no encontrado con id: " + id));
        return conceptoMapper.toResponse(concepto);
    }

    @Transactional
    public ConceptoResponse actualizarConcepto(Long id, ConceptoModificacionRequest request) {
        Concepto concepto = conceptoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concepto no encontrado con id: " + id));
        conceptoMapper.updateEntityFromRequest(request, concepto);
        SubConcepto subConcepto = subConceptoRepositorio.findById(request.subConceptoId())
                .orElseThrow(() -> new IllegalArgumentException("SubConcepto no encontrado con id: " + request.subConceptoId()));
        concepto.setSubConcepto(subConcepto);
        Concepto updated = conceptoRepositorio.save(concepto);
        return conceptoMapper.toResponse(updated);
    }

    @Transactional
    public void eliminarConcepto(Long id) {
        if (!conceptoRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Concepto no encontrado con id: " + id);
        }
        conceptoRepositorio.deleteById(id);
    }
}
