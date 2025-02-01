package ledance.dto.mappers;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BonificacionMapper {

    Bonificacion toEntity(BonificacionRequest request);

    BonificacionResponse toDTO(Bonificacion bonificacion);
}
