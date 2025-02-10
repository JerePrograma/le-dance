package ledance.dto.mappers;

import ledance.dto.request.ObservacionMensualRequest;
import ledance.dto.response.ObservacionMensualResponse;
import ledance.entidades.ObservacionMensual;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ObservacionMensualMapper {

    @Mapping(target = "alumnoId", source = "alumno.id")
    ObservacionMensualResponse toDTO(ObservacionMensual observacionMensual);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asistenciaMensual", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    ObservacionMensual toEntity(ObservacionMensualRequest request);
}