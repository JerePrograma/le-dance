package ledance.dto.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.asistencia.response.AsistenciaAlumnoMensualDetalleResponse;
import ledance.dto.asistencia.response.DisciplinaResponse;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.AsistenciaAlumnoMensual;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AsistenciaDiariaMapper.class})
public interface AsistenciaMensualMapper {

    // Mapea la entidad AsistenciaMensual a su respuesta detallada, anidando la información de disciplina y alumnos.
    @Mapping(target = "disciplina", source = "disciplina")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "alumnos", source = "asistenciasAlumnoMensual")
    AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual);

    // Mapea para listado; en este caso se utiliza un objeto anidado para la disciplina.
    @Mapping(target = "disciplina", source = "disciplina")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual);

    // Para crear una nueva planilla (se ignora la relación de alumnos, que se establecerá posteriormente)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "asistenciasAlumnoMensual", ignore = true)
    AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request);

    /**
     * Actualiza la planilla mensual sin modificar disciplina, mes ni año.
     * La actualización de los registros de alumno se hace por separado.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "mes", ignore = true)
    @Mapping(target = "anio", ignore = true)
    @Mapping(target = "asistenciasAlumnoMensual", ignore = true)
    void updateEntityFromRequest(AsistenciaMensualModificacionRequest request,
                                 @MappingTarget AsistenciaMensual asistenciaMensual);

    List<AsistenciaAlumnoMensualDetalleResponse> toAlumnoDetalleDTOList(List<AsistenciaAlumnoMensual> alumnos);

    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "observacion", source = "observacion")
    @Mapping(target = "asistenciaMensualId", source = "asistenciaMensual.id")
    @Mapping(target = "asistenciasDiarias", source = "asistenciasDiarias")
    AsistenciaAlumnoMensualDetalleResponse toAlumnoDetalleDTO(AsistenciaAlumnoMensual alumno);
}
