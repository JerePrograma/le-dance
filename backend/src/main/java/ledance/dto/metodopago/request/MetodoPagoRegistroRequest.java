// src/main/java/ledance/dto/metodopago/request/MetodoPagoRegistroRequest.java
package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;

public record MetodoPagoRegistroRequest(
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,
        Boolean activo,
        Double recargo
) { }
