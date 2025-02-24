// src/main/java/ledance/dto/metodopago/response/MetodoPagoResponse.java
package ledance.dto.metodopago.response;

public record MetodoPagoResponse(
        Long id,
        String descripcion,
        Boolean activo
) { }
