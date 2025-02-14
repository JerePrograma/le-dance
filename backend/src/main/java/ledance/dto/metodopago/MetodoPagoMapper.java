package ledance.dto.metodopago;

import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.request.MetodoPagoModificacionRequest;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.entidades.MetodoPago;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MetodoPagoMapper {

    /**
     * ✅ Convierte un `MetodoPago` a `MetodoPagoResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "descripcion", source = "descripcion")
    MetodoPagoResponse toDTO(MetodoPago metodoPago);

    /**
     * ✅ Convierte un `MetodoPagoRegistroRequest` en una entidad `MetodoPago`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // ✅ Se asigna automáticamente en true
    MetodoPago toEntity(MetodoPagoRegistroRequest request);

    /**
     * ✅ Actualiza un método de pago existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(MetodoPagoModificacionRequest request, @MappingTarget MetodoPago metodoPago);
}
