package ledance.dto.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.alumno.response.AlumnoResumenResponse;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.response.ObservacionMensualResponse;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Inscripcion;
import ledance.entidades.ObservacionMensual;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AsistenciaDiariaMapper.class})
public interface AsistenciaMensualMapper {

    /**
     * Mapea una entidad AsistenciaMensual a su detalle de respuesta.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "disciplina", source = "inscripcion.disciplina.nombre")
    @Mapping(target = "profesor", source = "inscripcion.disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    @Mapping(target = "asistenciasDiarias", source = "asistenciasDiarias")
    @Mapping(target = "observaciones", source = "observaciones")
    @Mapping(target = "disciplinaId", source = "inscripcion.disciplina.id")
    @Mapping(
            target = "alumnos",
            expression = "java(mapAlumnosToResumen(asistenciaMensual.getInscripcion().getDisciplina().getInscripciones()))"
    )
    AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual);

    /**
     * Convierte un request de registro a la entidad, ignorando datos que se setean luego.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "observaciones", ignore = true)
    AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request);

    /**
     * Actualiza una AsistenciaMensual existente con datos del request de modificación.
     * Se ignoran campos que no deben ser sobreescritos (por ejemplo: mes, año, etc.).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "mes", ignore = true)
    @Mapping(target = "anio", ignore = true)
    void updateEntityFromRequest(
            AsistenciaMensualModificacionRequest request,
            @MappingTarget AsistenciaMensual asistenciaMensual
    );

    /**
     * Mapea la entidad a la versión de listado, con campos básicos.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "disciplina", source = "inscripcion.disciplina.nombre")
    @Mapping(target = "profesor", source = "inscripcion.disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual);

    /**
     * Convierte una lista de inscripciones en respuestas de tipo AlumnoResumenResponse.
     */
    @Named("mapAlumnosToResumen")
    default List<AlumnoResumenResponse> mapAlumnosToResumen(List<Inscripcion> inscripciones) {
        return inscripciones.stream()
                .map(inscripcion -> new AlumnoResumenResponse(
                        inscripcion.getAlumno().getId(),
                        inscripcion.getAlumno().getNombre(),
                        inscripcion.getAlumno().getApellido()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de ObservacionMensual en respuestas de tipo ObservacionMensualResponse.
     */
    @Named("mapObservacionesToDTO")
    default List<ObservacionMensualResponse> mapObservacionesToDTO(List<ObservacionMensual> observaciones) {
        return observaciones.stream()
                .map(obs -> new ObservacionMensualResponse(
                        obs.getId(),
                        obs.getAlumno().getId(),
                        obs.getObservacion()
                ))
                .collect(Collectors.toList());
    }
}
