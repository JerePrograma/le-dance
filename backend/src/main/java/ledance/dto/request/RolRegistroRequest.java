package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RolRegistroRequest(
        @NotBlank String descripcion
) {}
