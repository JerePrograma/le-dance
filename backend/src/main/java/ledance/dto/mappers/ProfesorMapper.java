package ledance.dto.mappers;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.entidades.Profesor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "disciplinas", ignore = true)
    Profesor toEntity(ProfesorRegistroRequest request);

    // Mapeo a DTO para registro
    default DatosRegistroProfesorResponse toDatosRegistroDTO(Profesor profesor) {
        return new DatosRegistroProfesorResponse(
                profesor.getId(),
                profesor.getNombre(),
                profesor.getApellido(),
                profesor.getEspecialidad(),
                profesor.getAniosExperiencia()
        );
    }

    default ProfesorListadoResponse toListadoDTO(Profesor profesor) {
        return new ProfesorListadoResponse(profesor.getId(), profesor.getNombre(), profesor.getApellido());
    }
}
