package ledance.dto;

import ledance.entidades.Salon;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommonMapper {

    default String mapSalonToString(Salon salon) {
        return (salon != null) ? salon.getNombre() : null;
    }
}
