package ledance.dto.pago;

import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePagoMapper {

    // Mapeo a entidad (sin cambios)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", ignore = true)
    @Mapping(target = "importeInicial", ignore = true)
    @Mapping(target = "importePendiente", ignore = true)
    @Mapping(target = "AFavor", ignore = true)
    DetallePago toEntity(DetallePagoRegistroRequest request);

    // Mapeo a DTO: se asigna aCobrar el mismo valor que importePendiente.
    @Mapping(target = "bonificacionId", expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null)")
    @Mapping(target = "recargoId", expression = "java(detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null)")
    @Mapping(target = "importe", source = "importePendiente") // Utilizamos importePendiente como importe.
    @Mapping(target = "importeInicial", source = "importeInicial")
    @Mapping(target = "importePendiente", source = "importePendiente")
    @Mapping(target = "aCobrar", expression = "java(detallePago.getImportePendiente() != null ? detallePago.getImportePendiente() : 0.0)")
    DetallePagoResponse toDTO(DetallePago detallePago);
}
