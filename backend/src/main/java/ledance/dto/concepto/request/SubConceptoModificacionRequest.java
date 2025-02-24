package ledance.dto.concepto.request;

import jakarta.validation.constraints.NotNull;

public record SubConceptoModificacionRequest(
        @NotNull String descripcion
) {}
