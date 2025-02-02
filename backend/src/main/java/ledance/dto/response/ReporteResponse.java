package ledance.dto.response;

import java.time.LocalDate;

public record ReporteResponse(
        Long id,
        String tipo,
        String descripcion,
        LocalDate fechaGeneracion,
        Boolean activo
) {}
