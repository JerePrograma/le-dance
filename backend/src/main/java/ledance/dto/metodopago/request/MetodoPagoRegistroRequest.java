// src/main/java/ledance/dto/metodopago/request/MetodoPagoRegistroRequest.java
package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;

public record MetodoPagoRegistroRequest(
        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,
        // Puedes agregar validaciones o definirlo sin ellas si es opcional
        Double recargo
) { }
