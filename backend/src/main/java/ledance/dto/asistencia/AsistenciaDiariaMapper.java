package ledance.dto.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.entidades.AsistenciaDiaria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsistenciaDiariaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "alumnoNombre", source = "alumno.nombre")
    @Mapping(target = "alumnoApellido", source = "alumno.apellido")
    @Mapping(target = "asistenciaMensualId", source = "asistenciaMensual.id")
    @Mapping(target = "disciplinaId", source = "asistenciaMensual.inscripcion.disciplina.id")
    @Mapping(target = "observacion", source = "observacion")
    AsistenciaDiariaResponse toDTO(AsistenciaDiaria asistenciaDiaria);

    @Mapping(target = "id", ignore = false) // Permite actualizar si ya existe
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "asistenciaMensual", ignore = true)
    AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "asistenciaMensual", ignore = true)
    void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request, @org.mapstruct.MappingTarget AsistenciaDiaria asistenciaDiaria);
}
