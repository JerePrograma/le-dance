package ledance.dto.pago.response;

import java.time.LocalDate;

public record DetallePagoResponse(
        Long id,
        String descripcionConcepto,
        String cuota,
        Double montoOriginal,
        Long bonificacionId,
        Long recargoId,
        Double aFavor,
        Double aCobrar,
        Boolean cobrado,
        Long conceptoId,
        Long subConceptoId,
        Long mensualidadId,
        Long matriculaId,
        Long stockId,
        Double importeInicial,
        Double importePendiente,
        String tipo,           // Nuevo campo para indicar el tipo (MENSUALIDAD, MATRICULA, etc.)
        LocalDate fechaRegistro  // Nuevo campo para la fecha de registro
) {}
