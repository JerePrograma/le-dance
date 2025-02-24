// src/main/java/ledance/dto/metodopago/request/MetodoPagoModificacionRequest.java
package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MetodoPagoModificacionRequest(
        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) { }
