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

    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "activo", source = "activo") // ✅ Se agrega el mapeo de activo
    RolResponse toDTO(Rol rol);
}
