package ledance.dto.mappers;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.request.ProfesorModificacionRequest;
import ledance.dto.response.ProfesorDetalleResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.entidades.Profesor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "disciplinas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "telefono", source = "telefono")
    Profesor toEntity(ProfesorRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplinas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "telefono", source = "telefono")
    void updateEntityFromRequest(ProfesorModificacionRequest request, @MappingTarget Profesor profesor);

    ProfesorDetalleResponse toDetalleResponse(Profesor profesor);

    @Mapping(target = "id", source = "id")
    ProfesorListadoResponse toListadoResponse(Profesor profesor);


}

