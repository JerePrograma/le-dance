// src/main/java/ledance/dto/metodopago/request/MetodoPagoRegistroRequest.java
package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;

public record MetodoPagoRegistroRequest(
        @NotBlank(message = "La descripción es obligatoria")
        String descripcion
) { }
