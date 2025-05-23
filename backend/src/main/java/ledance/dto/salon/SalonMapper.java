package ledance.dto.salon;

import ledance.dto.salon.request.SalonModificacionRequest;
import ledance.dto.salon.request.SalonRegistroRequest;
import ledance.dto.salon.response.SalonResponse;
import ledance.entidades.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalonMapper {

    // Convierte de Request a Entidad
    @Mapping(target = "id", ignore = true) // El ID se genera automaticamente
    Salon toEntity(SalonRegistroRequest request);

    // Para modificacion (puede compartir la misma logica)
    @Mapping(target = "id", ignore = true)
    Salon toEntity(SalonModificacionRequest request);

    // Convierte la entidad a Response
    SalonResponse toResponse(Salon entity);

}
