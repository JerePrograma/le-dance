package ledance.dto.metodopago.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para registrar un nuevo método de pago.
 * - `activo` se asigna automáticamente en el servicio.
 */
public record MetodoPagoRegistroRequest(
        @NotBlank String descripcion // Nombre del método de pago (Efectivo, Transferencia, etc.)
) {}
