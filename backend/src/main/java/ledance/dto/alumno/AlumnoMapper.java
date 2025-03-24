package ledance.dto.alumno;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.entidades.Alumno;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {InscripcionMapper.class})
public interface AlumnoMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "celular1", source = "celular1")
    @Mapping(target = "apellido", source = "apellido")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "inscripciones", ignore = true)
    AlumnoResponse toResponse(Alumno alumno);

    @Mapping(target = "inscripciones", ignore = true)
    AlumnoResponse toSimpleResponse(Alumno alumno);

    @Mapping(target = "activo", constant = "true")
    Alumno toEntity(AlumnoRegistroRequest request);

    void updateEntityFromRequest(AlumnoRegistroRequest request, @MappingTarget Alumno alumno);

    AlumnoListadoResponse toAlumnoListadoResponse(Alumno alumno);

}