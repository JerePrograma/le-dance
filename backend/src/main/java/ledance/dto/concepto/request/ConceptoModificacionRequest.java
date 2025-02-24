package ledance.dto.concepto.request;

import jakarta.validation.constraints.NotNull;

public record ConceptoModificacionRequest(
        @NotNull String descripcion,
        double precio,
        @NotNull Long subConceptoId
) {}
