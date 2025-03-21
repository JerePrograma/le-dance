package ledance.dto.asistencia;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaDetalleResponse;
import ledance.entidades.AsistenciaDiaria;
import ledance.entidades.Inscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsistenciaDiariaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "fecha", source = "fecha")
    @Mapping(target = "estado", source = "estado")
    // Se asigna el objeto AlumnoResponse a partir de la inscripcion del alumno
    @Mapping(target = "alumno", source = "asistenciaAlumnoMensual.inscripcion")
    // Se obtiene el id del registro mensual del alumno
    @Mapping(target = "asistenciaAlumnoMensualId", source = "asistenciaAlumnoMensual.id")
    @Mapping(target = "asistenciaMensualId", source = "asistenciaAlumnoMensual.asistenciaMensual.id")
    @Mapping(target = "disciplinaId", source = "asistenciaAlumnoMensual.asistenciaMensual.disciplina.id")
    AsistenciaDiariaDetalleResponse toDTO(AsistenciaDiaria asistenciaDiaria);

    @Mapping(target = "id", ignore = false)
    // La relacion con asistenciaAlumnoMensual se establecerá por separado en el servicio
    @Mapping(target = "asistenciaAlumnoMensual", ignore = true)
    AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "asistenciaAlumnoMensual", ignore = true)
    void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request,
                                 @org.mapstruct.MappingTarget AsistenciaDiaria asistenciaDiaria);

}
