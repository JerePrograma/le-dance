package ledance.dto.concepto.request;

import jakarta.validation.constraints.NotNull;

public record SubConceptoRegistroRequest(
        @NotNull String descripcion
) {}
