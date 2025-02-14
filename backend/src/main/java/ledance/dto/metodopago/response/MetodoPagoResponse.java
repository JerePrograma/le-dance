package ledance.dto.metodopago.response;

public record MetodoPagoResponse(
        Long id, // Identificador único del método de pago
        String descripcion, // Nombre del método de pago
        Boolean activo // Estado (activo/inactivo)
) {
}