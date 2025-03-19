package ledance.controladores;

import ledance.dto.concepto.request.ConceptoRegistroRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.servicios.concepto.ConceptoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conceptos")
@Validated
public class ConceptoControlador {

    private static final Logger log = LoggerFactory.getLogger(ConceptoControlador.class);
    private final ConceptoServicio conceptoServicio;

    public ConceptoControlador(ConceptoServicio conceptoServicio) {
        this.conceptoServicio = conceptoServicio;
    }

    @PostMapping
    public ResponseEntity<ConceptoResponse> crearConcepto(@RequestBody @Validated ConceptoRegistroRequest request) {
        log.info("Creando concepto: {}", request.descripcion());
        ConceptoResponse nuevo = conceptoServicio.crearConcepto(request);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<ConceptoResponse>> listarConceptos() {
        List<ConceptoResponse> conceptos = conceptoServicio.listarConceptos();
        return ResponseEntity.ok(conceptos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConceptoResponse> obtenerConceptoPorId(@PathVariable Long id) {
        ConceptoResponse concepto = conceptoServicio.obtenerConceptoPorId(id);
        return ResponseEntity.ok(concepto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConceptoResponse> actualizarConcepto(@PathVariable Long id,
                                                               @RequestBody @Validated ConceptoRegistroRequest request) {
        ConceptoResponse actualizado = conceptoServicio.actualizarConcepto(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarConcepto(@PathVariable Long id) {
        conceptoServicio.eliminarConcepto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sub-concepto/{subConceptoId}")
    public ResponseEntity<List<ConceptoResponse>> listarConceptosPorSubConcepto(@PathVariable Long subConceptoId) {
        log.info("Listando conceptos para el subconcepto con id: {}", subConceptoId);
        List<ConceptoResponse> conceptos = conceptoServicio.listarConceptosPorSubConcepto(subConceptoId);
        return conceptos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(conceptos);
    }
}
