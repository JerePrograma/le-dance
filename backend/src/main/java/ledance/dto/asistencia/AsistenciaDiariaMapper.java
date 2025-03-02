package ledance.dto.asistencia;

import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaResponse;
import ledance.entidades.AsistenciaDiaria;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AsistenciaDiariaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "alumnoNombre", source = "alumno.nombre")
    @Mapping(target = "alumnoApellido", source = "alumno.apellido")
    @Mapping(target = "disciplinaHorarioId", source = "disciplinaHorario.id") // ✅ Nueva relación
    @Mapping(target = "horarioInicio", source = "disciplinaHorario.horarioInicio") // ✅ Se añade hora de inicio
    AsistenciaDiariaResponse toDTO(AsistenciaDiaria asistenciaDiaria);

    @Mapping(target = "id", ignore = false)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplinaHorario", ignore = true) // ✅ Se ignora para asignarse en el servicio
    AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplinaHorario", ignore = true) // ✅ Se ignora para evitar sobrescribir la relación
    void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request, @MappingTarget AsistenciaDiaria asistenciaDiaria);
}
