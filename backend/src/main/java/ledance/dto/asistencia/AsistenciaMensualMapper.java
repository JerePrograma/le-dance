package ledance.dto.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.response.*;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.AsistenciaAlumnoMensual;
import ledance.entidades.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AsistenciaDiariaMapper.class})
public interface AsistenciaMensualMapper {

    @Mapping(target = "disciplina", source = "disciplina")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "alumnos", source = "asistenciasAlumnoMensual")
    AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual);

    // Método de mapeo para convertir Salon a String (su nombre)
    default String map(Salon salon) {
        return (salon != null) ? salon.getNombre() : null;
    }

    // Mapea para listado; en este caso se utiliza un objeto anidado para la disciplina.
    @Mapping(target = "disciplina", source = "disciplina")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual);

    // Para crear una nueva planilla (se ignora la relacion de alumnos, que se establecerá posteriormente)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "asistenciasAlumnoMensual", ignore = true)
    AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request);

    /**
     * Actualiza la planilla mensual sin modificar disciplina, mes ni año.
     * La actualizacion de los registros de alumno se hace por separado.
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
    @Mapping(target = "alumno", source = "alumno")
    AsistenciaAlumnoMensualDetalleResponse toAlumnoDetalleDTO(AsistenciaAlumnoMensual alumno);

}
