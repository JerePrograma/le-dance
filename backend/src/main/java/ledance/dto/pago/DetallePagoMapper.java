package ledance.dto.pago;

import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePagoMapper {

    // Mapeo de DTO a Entity
    @Mapping(target = "concepto", ignore = true)
    @Mapping(target = "subConcepto", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", ignore = true)
    @Mapping(target = "importeInicial", ignore = true)
    @Mapping(target = "importePendiente", ignore = true)
    @Mapping(target = "AFavor", ignore = true)
    @Mapping(target = "mensualidad", ignore = true)
    @Mapping(target = "matricula", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "descripcionConcepto", source = "descripcionConcepto")
    DetallePago toEntity(DetallePagoRegistroRequest request);

    // Mapeo de Entity a DTO de respuesta
    @Mapping(target = "bonificacionId", expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null)")
    @Mapping(target = "recargoId", expression = "java(detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null)")
    @Mapping(target = "aFavor", source = "AFavor")
    @Mapping(target = "importeInicial", source = "importeInicial")
    @Mapping(target = "importePendiente", source = "importePendiente")
    @Mapping(target = "aCobrar", source = "aCobrar")
    @Mapping(target = "montoOriginal", source = "montoOriginal")
    @Mapping(target = "descripcionConcepto", source = "descripcionConcepto")
    @Mapping(target = "mensualidadId", expression = "java(detallePago.getMensualidad() != null ? detallePago.getMensualidad().getId() : null)")
    @Mapping(target = "matriculaId", expression = "java(detallePago.getMatricula() != null ? detallePago.getMatricula().getId() : null)")
    @Mapping(target = "stockId", expression = "java(detallePago.getStock() != null ? detallePago.getStock().getId() : null)")
    // Mapeo de campos nuevos:
    @Mapping(target = "tipo", expression = "java(detallePago.getTipo() != null ? detallePago.getTipo().name() : null)")
    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    DetallePagoResponse toDTO(DetallePago detallePago);
}
