package ledance.dto.mappers;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;
import ledance.entidades.Disciplina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisciplinaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profesor", ignore = true)
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "inscripciones", ignore = true) // ✅ Se ignoran las inscripciones
    Disciplina toEntity(DisciplinaRequest request);

    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "inscritos", expression = "java(disciplina.getInscripciones() != null ? disciplina.getInscripciones().size() : 0)")
    DisciplinaResponse toDTO(Disciplina disciplina);
}
