package ledance.controladores;

import ledance.dto.concepto.ConceptoMapper;
import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.Concepto;
import ledance.entidades.SubConcepto;
import ledance.repositorios.ConceptoRepositorio;
import ledance.servicios.concepto.SubConceptoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sub-conceptos")
@Validated
public class SubConceptoControlador {

    private static final Logger log = LoggerFactory.getLogger(SubConceptoControlador.class);
    private final SubConceptoServicio subConceptoServicio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final ConceptoMapper conceptoMapper;

    public SubConceptoControlador(SubConceptoServicio subConceptoServicio, ConceptoRepositorio conceptoRepositorio, ConceptoMapper conceptoMapper) {
        this.subConceptoServicio = subConceptoServicio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.conceptoMapper = conceptoMapper;
    }

    @PostMapping
    public ResponseEntity<SubConceptoResponse> crearSubConcepto(@RequestBody @Validated SubConceptoRegistroRequest request) {
        log.info("Creando subconcepto: {}", request.descripcion());
        SubConceptoResponse nuevo = subConceptoServicio.crearSubConcepto(request);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<SubConceptoResponse>> listarSubConceptos() {
        List<SubConceptoResponse> subconceptos = subConceptoServicio.listarSubConceptos();
        return ResponseEntity.ok(subconceptos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubConceptoResponse> obtenerSubConceptoPorId(@PathVariable Long id) {
        SubConceptoResponse subconcepto = subConceptoServicio.obtenerSubConceptoPorId(id);
        return ResponseEntity.ok(subconcepto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubConceptoResponse> actualizarSubConcepto(@PathVariable Long id,
                                                                     @RequestBody @Validated SubConceptoRegistroRequest request) {
        SubConceptoResponse actualizado = subConceptoServicio.actualizarSubConcepto(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSubConcepto(@PathVariable Long id) {
        subConceptoServicio.eliminarSubConcepto(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint para busqueda de subconceptos por nombre (para sugerencias)
    @GetMapping("/buscar")
    public ResponseEntity<List<SubConceptoResponse>> buscarPorNombre(@RequestParam String nombre) {
        List<SubConceptoResponse> resultado = subConceptoServicio.buscarPorNombre(nombre);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{subConceptoDesc}")
    public ResponseEntity<List<ConceptoResponse>> listarConceptosPorSubConcepto(
            @PathVariable("subConceptoDesc") String subConceptoDesc) {

        SubConcepto subConcepto = subConceptoServicio.findByDescripcionIgnoreCase(subConceptoDesc);
        if (subConcepto == null) {
            return ResponseEntity.notFound().build();
        }
        List<Concepto> conceptos = conceptoRepositorio.findBySubConceptoId(subConcepto.getId());
        List<ConceptoResponse> responses = conceptos.stream()
                .map(conceptoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

}
