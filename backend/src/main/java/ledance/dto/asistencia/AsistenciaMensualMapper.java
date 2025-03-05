package ledance.dto.asistencia;

import ledance.dto.alumno.response.AlumnoResumenResponse;
import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AsistenciaDiariaMapper.class})
public interface AsistenciaMensualMapper {

    /**
     * Mapea una entidad AsistenciaMensual a su detalle de respuesta.
     * Se obtienen los datos de la disciplina y profesor directamente.
     */
    @Mapping(target = "disciplina", source = "disciplina.nombre")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "disciplinaId", source = "disciplina.id")
    @Mapping(target = "alumnos", expression = "java(mapAlumnosToResumen(asistenciaMensual.getDisciplina().getInscripciones()))")
    AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual);

    /**
     * Mapea la entidad a la versión de listado.
     */
    @Mapping(target = "disciplinaId", source = "disciplina.id")
    @Mapping(target = "disciplina", source = "disciplina.nombre")
    @Mapping(target = "profesor", source = "disciplina.profesor.nombre")
    @Mapping(target = "mes", source = "mes")
    @Mapping(target = "anio", source = "anio")
    AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual);

    /**
     * Convierte un request de registro a la entidad.
     * Nota: La creación de la planilla se manejará en el servicio, por lo que algunos campos se asignan allí.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "observacion", ignore = true)
    AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request);

    /**
     * Actualiza una entidad existente con datos del request de modificación.
     * Se ignoran campos críticos como mes, anio y disciplina.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "asistenciasDiarias", ignore = true)
    @Mapping(target = "mes", ignore = true)
    @Mapping(target = "anio", ignore = true)
    void updateEntityFromRequest(AsistenciaMensualModificacionRequest request, @MappingTarget AsistenciaMensual asistenciaMensual);

    /**
     * Método auxiliar para mapear la lista de inscripciones de la disciplina a una lista de resumen de alumnos.
     * Se asume que la entidad Disciplina tiene una relación con Inscripcion.
     */
    default List<AlumnoResumenResponse> mapAlumnosToResumen(List<Inscripcion> inscripciones) {
        if (inscripciones == null) {
            return null;
        }
        return inscripciones.stream()
                .map(inscripcion -> {
                    if (inscripcion == null || inscripcion.getAlumno() == null) {
                        return null;
                    }
                    return new AlumnoResumenResponse(
                            inscripcion.getAlumno().getId(),
                            inscripcion.getAlumno().getNombre(),
                            inscripcion.getAlumno().getApellido()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
