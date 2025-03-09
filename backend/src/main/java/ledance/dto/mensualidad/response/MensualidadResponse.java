package ledance.dto.mensualidad.response;

import java.time.LocalDate;

public record MensualidadResponse(
        Long id,
        LocalDate fechaCuota,
        Double valorBase,
        Long recargoId, // Se devuelve el ID del recargo
        Long bonificacionId, // Se devuelve el ID de la bonificación
        String estado,
        Long inscripcionId,
        Double totalPagar,  // Nuevo campo agregado
        String descripcion
) {
}
