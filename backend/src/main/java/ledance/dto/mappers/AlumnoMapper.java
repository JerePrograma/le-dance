package ledance.dto.mappers;

import ledance.dto.request.AlumnoRegistroRequest;
import ledance.dto.request.AlumnoModificacionRequest;
import ledance.dto.response.AlumnoDetalleResponse;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.entidades.Alumno;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {

    AlumnoDetalleResponse toDetalleResponse(Alumno alumno);

    AlumnoListadoResponse toListadoResponse(Alumno alumno);

    @Mapping(target = "activo", constant = "true")
    Alumno toEntity(AlumnoRegistroRequest request);

    void updateEntityFromRequest(AlumnoModificacionRequest request, @MappingTarget Alumno alumno);

}

