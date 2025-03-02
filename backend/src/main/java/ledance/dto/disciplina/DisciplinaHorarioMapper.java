package ledance.dto.disciplina;

import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.entidades.DisciplinaHorario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DisciplinaHorarioMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "diaSemana", source = "diaSemana")
    @Mapping(target = "horarioInicio", source = "horarioInicio")
    @Mapping(target = "duracion", source = "duracion")
    DisciplinaHorarioResponse toResponse(DisciplinaHorario horario);

    @Named("toResponseList")
    default List<DisciplinaHorarioResponse> toResponseList(List<DisciplinaHorario> horarios) {
        return horarios.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Mapping(target = "id", ignore = true)
    DisciplinaHorario toEntity(DisciplinaHorarioRequest request);
}
