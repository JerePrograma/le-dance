package ledance.dto.mappers;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Inscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    // No se mapean automáticamente las asociaciones:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    Inscripcion toEntity(InscripcionRequest request);

    // Para el DTO, se usan expresiones para mapear las asociaciones
    @Mapping(target = "alumno", expression = "java(new AlumnoListadoResponse(inscripcion.getAlumno().getId(), inscripcion.getAlumno().getNombre(), inscripcion.getAlumno().getApellido()))")
    @Mapping(target = "disciplina", expression = "java(new DisciplinaSimpleResponse(inscripcion.getDisciplina().getId(), inscripcion.getDisciplina().getNombre()))")
    @Mapping(target = "bonificacion", expression = "java(inscripcion.getBonificacion() != null ? new BonificacionResponse(inscripcion.getBonificacion().getId(), inscripcion.getBonificacion().getDescripcion(), inscripcion.getBonificacion().getPorcentajeDescuento(), inscripcion.getBonificacion().getActivo(), inscripcion.getBonificacion().getObservaciones()) : null)")
    InscripcionResponse toDTO(Inscripcion inscripcion);
}
