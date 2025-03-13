package ledance.dto.pago;

import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePagoMapper {

    // En el mapping a entidad, se ignoran los campos que se calcularán en el backend.
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", ignore = true)
    // Los campos 'importeInicial' e 'importePendiente' no se reciben desde el request.
    @Mapping(target = "importeInicial", ignore = true)
    @Mapping(target = "importePendiente", ignore = true)
    @Mapping(target = "AFavor", ignore = true)  // Si se calcula o se asigna por defecto.
    DetallePago toEntity(DetallePagoRegistroRequest request);

    // Al convertir a DTO, se mapean los campos nuevos
    @Mapping(target = "bonificacionId", expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null)")
    @Mapping(target = "recargoId", expression = "java(detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null)")
    @Mapping(target = "importe", source = "importePendiente") // Para el control actual, puede utilizarse el importe pendiente.
    @Mapping(target = "importeInicial", source = "importeInicial")
    @Mapping(target = "importePendiente", source = "importePendiente")
    DetallePagoResponse toDTO(DetallePago detallePago);
}
