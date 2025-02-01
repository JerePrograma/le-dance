package ledance.dto.mappers;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.entidades.Alumno;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {
    // MapStruct inyecta el bean automáticamente
    @Mapping(target = "edad", ignore = true) // Se calcula en el servicio
    Alumno toEntity(AlumnoRequest request);

    AlumnoResponse toDTO(Alumno alumno);

    AlumnoListadoResponse toListadoResponse(Alumno alumno);

    // Método para actualizar una entidad existente; MapStruct genera la implementación
    void updateEntityFromRequest(Alumno alumno, AlumnoRequest request);
}
