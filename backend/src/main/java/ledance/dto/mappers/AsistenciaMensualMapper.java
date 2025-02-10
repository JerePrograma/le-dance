package ledance.dto.mappers;

import ledance.dto.request.AsistenciaMensualRegistroRequest;
import ledance.dto.request.AsistenciaMensualModificacionRequest;
import ledance.dto.response.AlumnoResumenResponse;
import ledance.dto.response.AsistenciaMensualDetalleResponse;
import ledance.dto.response.AsistenciaMensualListadoResponse;
import ledance.dto.response.ObservacionMensualResponse;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Inscripcion;
import ledance.entidades.ObservacionMensual;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AsistenciaDiariaMapper.class})
public interface AsistenciaMensualMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "disciplina", source = "inscripcion.disciplina.nombre")
    @Mapping(target = "profesor", source = "inscripcion.disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    @Mapping(target = "asistenciasDiarias", source = "asistenciasDiarias")
    @Mapping(target = "observaciones", source = "observaciones")
    @Mapping(target = "totalClases", source = "inscripcion.disciplina.frecuenciaSemanal") // ✅ Agregado
    @Mapping(target = "disciplinaId", source = "inscripcion.disciplina.id")
    @Mapping(target = "alumnos", expression = "java(mapAlumnosToResumen(asistenciaMensual.getInscripcion().getDisciplina().getInscripciones()))")
        // ✅ Ahora mapea correctamente la lista de alumnos
    AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "observaciones", ignore = true)
    AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "mes", ignore = true)
    @Mapping(target = "anio", ignore = true)
    void updateEntityFromRequest(AsistenciaMensualModificacionRequest request, @MappingTarget AsistenciaMensual asistenciaMensual);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "disciplina", source = "inscripcion.disciplina.nombre")
    @Mapping(target = "profesor", source = "inscripcion.disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual);

    @Named("mapAlumnosToResumen")
    default List<AlumnoResumenResponse> mapAlumnosToResumen(List<Inscripcion> inscripciones) {
        return inscripciones.stream()
                .map(inscripcion -> new AlumnoResumenResponse(inscripcion.getAlumno().getId(), inscripcion.getAlumno().getNombre(), inscripcion.getAlumno().getApellido()))
                .collect(Collectors.toList());
    }

    @Named("mapObservacionesToDTO")
    default List<ObservacionMensualResponse> mapObservacionesToDTO(List<ObservacionMensual> observaciones) {
        return observaciones.stream()
                .map(obs -> new ObservacionMensualResponse(obs.getId(), obs.getAlumno().getId(), obs.getObservacion()))
                .collect(Collectors.toList());
    }
}
