package ledance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SalonRegistroRequest(
        @NotBlank String nombre,
        String descripcion
) {}
