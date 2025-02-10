package ledance.dto.mappers;

import ledance.dto.request.SalonModificacionRequest;
import ledance.dto.request.SalonRegistroRequest;
import ledance.dto.response.SalonResponse;
import ledance.entidades.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalonMapper {

    // Convierte de Request a Entidad
    @Mapping(target = "id", ignore = true) // El ID se genera automáticamente
    Salon toEntity(SalonRegistroRequest request);

    // Para modificación (puede compartir la misma lógica)
    @Mapping(target = "id", ignore = true)
    Salon toEntity(SalonModificacionRequest request);

    // Convierte la entidad a Response
    SalonResponse toResponse(Salon entity);

}
