package ledance.servicios.concepto;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.concepto.request.ConceptoRegistroRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.concepto.ConceptoMapper;
import ledance.entidades.Concepto;
import ledance.entidades.SubConcepto;
import ledance.repositorios.ConceptoRepositorio;
import ledance.repositorios.SubConceptoRepositorio;
import ledance.servicios.disciplina.DisciplinaHorarioServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConceptoServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaHorarioServicio.class);

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
        SubConcepto subConcepto = subConceptoRepositorio
                .findById(request.subConcepto().id())
                .orElseThrow(() -> new EntityNotFoundException("SubConcepto no encontrado"));
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
    public ConceptoResponse actualizarConcepto(Long id, ConceptoRegistroRequest request) {
        log.info("[ConceptoService] Iniciando actualización del concepto con id: {}", id);

        Concepto concepto = conceptoRepositorio.findById(id)
                .orElseThrow(() -> {
                    String mensaje = "Concepto no encontrado con id: " + id;
                    log.error("[ConceptoService] {}", mensaje);
                    return new IllegalArgumentException(mensaje);
                });
        log.info("[ConceptoService] Concepto encontrado: {}", concepto);

        // Actualizamos solo los campos de Concepto, ignorando la descripción del subconcepto
        concepto.setDescripcion(request.descripcion());
        concepto.setPrecio(request.precio());
        log.info("[ConceptoService] Campos actualizados - Descripción: {}, Precio: {}", request.descripcion(), request.precio());

        // Recuperamos el subconcepto existente según el id del request
        Long subConceptoId = request.subConcepto().id();
        SubConcepto subConcepto = subConceptoRepositorio.findById(subConceptoId)
                .orElseThrow(() -> {
                    String mensaje = "SubConcepto no encontrado con id: " + subConceptoId;
                    log.error("[ConceptoService] {}", mensaje);
                    return new IllegalArgumentException(mensaje);
                });
        log.info("[ConceptoService] SubConcepto encontrado: {}", subConcepto);

        // Asignamos el subconcepto sin modificar su descripción
        concepto.setSubConcepto(subConcepto);

        Concepto updated = conceptoRepositorio.save(concepto);
        log.info("[ConceptoService] Concepto actualizado correctamente: {}", updated);

        ConceptoResponse response = conceptoMapper.toResponse(updated);
        log.info("[ConceptoService] Respuesta mapeada: {}", response);
        return response;
    }

    @Transactional
    public void eliminarConcepto(Long id) {
        if (!conceptoRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Concepto no encontrado con id: " + id);
        }
        conceptoRepositorio.deleteById(id);
    }

    public List<ConceptoResponse> listarConceptosPorSubConcepto(Long subConceptoId) {
        List<Concepto> conceptos = conceptoRepositorio.findBySubConceptoId(subConceptoId);
        return conceptos.stream()
                .map(conceptoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
