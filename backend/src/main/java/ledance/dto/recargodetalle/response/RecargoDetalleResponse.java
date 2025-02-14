// ledance/dto/response/RecargoDetalleResponse.java
package ledance.dto.recargodetalle.response;

public record RecargoDetalleResponse(
        Long id,
        Integer diaDesde,
        Double porcentaje
) {
}