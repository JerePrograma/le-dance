package ledance.dto.concepto.request;

import jakarta.validation.constraints.NotNull;

public record ConceptoRegistroRequest(
        @NotNull String descripcion,
        double precio,
        SubConceptoRegistroRequest subConcepto,
        Boolean activo
) {
}
