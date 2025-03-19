package ledance.dto.matricula;

import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.Matricula;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MatriculaMapper {

    MatriculaMapper INSTANCE = Mappers.getMapper(MatriculaMapper.class);

    @Mapping(target = "alumnoId", source = "alumno.id")
    MatriculaResponse toResponse(Matricula matricula);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pagada", constant = "false")
    @Mapping(target = "fechaPago", ignore = true)
    @Mapping(target = "alumno.id", source = "alumnoId")
    Matricula toEntity(MatriculaRegistroRequest request);

    @Mapping(target = "alumno.id", source = "alumnoId")
    Matricula toEntity(MatriculaResponse response);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    void updateEntityFromRequest(MatriculaRegistroRequest request, @MappingTarget Matricula matricula);
}
