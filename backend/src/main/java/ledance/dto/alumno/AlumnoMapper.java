package ledance.dto.alumno;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.request.AlumnoModificacionRequest;
import ledance.dto.alumno.response.AlumnoDetalleResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
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