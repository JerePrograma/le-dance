// ledance/dto/response/RecargoResponse.java
package ledance.dto.recargodetalle.response;

import java.util.List;

public record RecargoResponse(
        Long id,
        String descripcion,
        List<RecargoDetalleResponse> detalles
) {
}