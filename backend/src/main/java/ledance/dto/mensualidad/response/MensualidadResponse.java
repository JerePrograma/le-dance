package ledance.dto.mensualidad.response;

import ledance.dto.bonificacion.response.BonificacionResponse;

import java.time.LocalDate;

public record MensualidadResponse(
        Long id,
        LocalDate fechaCuota,
        Double valorBase,
        Long recargoId, // Se devuelve el ID del recargo
        BonificacionResponse bonificacion, // Se devuelve el ID de la bonificacion
        String estado,
        Long inscripcionId,
        Double importeInicial,  // Nuevo campo agregado
        String descripcion,
        Double importePendiente
) {
}
