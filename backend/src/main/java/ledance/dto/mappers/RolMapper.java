package ledance.dto.mappers;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.response.RolResponse;
import ledance.entidades.Rol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RolMapper {

    @Mapping(target = "id", ignore = true)
    Rol toEntity(RolRegistroRequest request);

    RolResponse toDTO(Rol rol);
}
