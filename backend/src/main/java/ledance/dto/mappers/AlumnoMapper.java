package ledance.dto.mappers;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.entidades.Alumno;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {

    @Mapping(target = "id", ignore = true) // Se ignora porque es autogenerado
    @Mapping(target = "edad", ignore = true) // Se calcula en el servicio
    @Mapping(target = "inscripciones", ignore = true) // Se manejan en otro mapeo
    Alumno toEntity(AlumnoRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellido", source = "apellido") // ✅ MapStruct lo mapea automaticamente
    @Mapping(target = "edad", source = "edad")
    @Mapping(target = "inscripciones", ignore = true) // ✅ Ignorar si no esta en `AlumnoResponse`
    AlumnoResponse toDTO(Alumno alumno);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellido", source = "apellido")
    AlumnoListadoResponse toListadoResponse(Alumno alumno);

    @Mapping(target = "edad", ignore = true) // ✅ Se ignora la edad ya que se calcula en el servicio
    @Mapping(target = "inscripciones", ignore = true)
    @Mapping(target = "id", ignore = true) // ✅ Se ignora la ID en la actualizacion
    void updateEntityFromRequest(@MappingTarget Alumno alumno, AlumnoRequest request);
}

