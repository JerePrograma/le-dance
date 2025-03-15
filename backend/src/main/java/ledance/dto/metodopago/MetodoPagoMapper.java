// src/main/java/ledance/dto/metodopago/MetodoPagoMapper.java
package ledance.dto.metodopago;

import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.entidades.MetodoPago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MetodoPagoMapper {

    MetodoPago toEntity(MetodoPagoRegistroRequest request);

    MetodoPagoResponse toDTO(MetodoPago metodoPago);

    // MapStruct actualizara recargo automaticamente si el campo tiene el mismo nombre
    void updateEntityFromRequest(MetodoPagoRegistroRequest request, @MappingTarget MetodoPago metodoPago);
}
