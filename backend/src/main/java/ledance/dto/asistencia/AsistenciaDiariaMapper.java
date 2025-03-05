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
    // Actualizado para obtener el disciplinaId desde la relación directa: asistenciaMensual.disciplina.id
    @Mapping(target = "disciplinaId", source = "asistenciaMensual.disciplina.id")
    AsistenciaDiariaResponse toDTO(AsistenciaDiaria asistenciaDiaria);

    @Mapping(target = "id", ignore = false) // Se permite la actualización si ya existe
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "asistenciaMensual", ignore = true)
    AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "asistenciaMensual", ignore = true)
    void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request, @org.mapstruct.MappingTarget AsistenciaDiaria asistenciaDiaria);
}
