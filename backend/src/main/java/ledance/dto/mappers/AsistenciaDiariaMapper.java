package ledance.dto.mappers;

import ledance.dto.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.response.AsistenciaDiariaResponse;
import ledance.entidades.AsistenciaDiaria;
import ledance.entidades.EstadoAsistencia;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AsistenciaDiariaMapper {

    @Mapping(target = "estado", source = "estado") // ✅ Ahora devuelve EstadoAsistencia en lugar de String
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "alumnoNombre", source = "alumno.nombre")
    @Mapping(target = "alumnoApellido", source = "alumno.apellido")
    @Mapping(target = "asistenciaMensualId", source = "asistenciaMensual.id")
    @Mapping(target = "disciplinaId", source = "asistenciaMensual.inscripcion.disciplina.id") // ✅ Agregado
    AsistenciaDiariaResponse toDTO(AsistenciaDiaria asistenciaDiaria);

    @Mapping(target = "id", source = "id") // ✅ Ahora se permite actualizar asistencias existentes
    @Mapping(target = "alumno", ignore = true) // Se debe manejar en el servicio
    @Mapping(target = "asistenciaMensual", ignore = true) // Se debe manejar en el servicio
    AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request);

    @Mapping(target = "id", source = "id") // ✅ Se asegura que el ID sea mapeado correctamente
    @Mapping(target = "alumno", ignore = true) // Se debe manejar en el servicio
    @Mapping(target = "asistenciaMensual", ignore = true) // Se debe manejar en el servicio
    void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request, @MappingTarget AsistenciaDiaria asistenciaDiaria);
}