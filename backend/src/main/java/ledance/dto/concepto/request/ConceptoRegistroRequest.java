package ledance.dto.concepto.request;

import jakarta.validation.constraints.NotNull;

public record ConceptoRegistroRequest(
        @NotNull String descripcion,
        double precio,
        @NotNull Long subConceptoId
) {}
