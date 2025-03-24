package ledance.dto.recargo.response;

public record RecargoResponse(
        Long id,
        String descripcion,
        Double porcentaje,
        Double valorFijo,
        Integer diaDelMesAplicacion // ✅ Dia especifico del mes
) {}
