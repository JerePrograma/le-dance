package ledance.dto.concepto.response;

public record ConceptoResponse(
        Long id,
        String descripcion,
        double precio,
        SubConceptoResponse subConcepto
) {}
