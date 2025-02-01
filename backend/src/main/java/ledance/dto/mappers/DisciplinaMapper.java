package ledance.dto.mappers;

import ledance.dto.request.DisciplinaRequest;
import ledance.dto.response.DisciplinaResponse;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.entidades.Disciplina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisciplinaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profesor", ignore = true) // La asociación se establece en el servicio
    @Mapping(target = "inscripciones", ignore = true)
    Disciplina toEntity(DisciplinaRequest request);

    // Para la respuesta, convertimos el campo profesor a profesorId
    @Mapping(target = "profesorId", expression = "java(disciplina.getProfesor() != null ? disciplina.getProfesor().getId() : null)")
    @Mapping(target = "inscritos", constant = "0") // Se puede actualizar en el servicio si se cuenta inscripciones
    DisciplinaResponse toDTO(Disciplina disciplina);

    default DisciplinaSimpleResponse toSimpleDTO(Disciplina disciplina) {
        return new DisciplinaSimpleResponse(disciplina.getId(), disciplina.getNombre());
    }
}
