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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true) // Se asigna en el servicio
    @Mapping(target = "disciplina", ignore = true) // Se asigna en el servicio
    @Mapping(target = "bonificacion", ignore = true) // Se asigna en el servicio si esta presente
    Inscripcion toEntity(InscripcionRequest request);

    @Mapping(target = "alumno", expression = "java(inscripcion.getAlumno() != null ? new AlumnoListadoResponse(inscripcion.getAlumno().getId(), inscripcion.getAlumno().getNombre(), inscripcion.getAlumno().getApellido(), inscripcion.getAlumno().getActivo()) : null)")
    @Mapping(target = "disciplina", expression = "java(inscripcion.getDisciplina() != null ? new DisciplinaSimpleResponse(inscripcion.getDisciplina().getId(), inscripcion.getDisciplina().getNombre()) : null)")
    @Mapping(target = "bonificacion", expression = "java(inscripcion.getBonificacion() != null ? new BonificacionResponse(inscripcion.getBonificacion().getId(), inscripcion.getBonificacion().getDescripcion(), inscripcion.getBonificacion().getPorcentajeDescuento(), inscripcion.getBonificacion().getActivo(), inscripcion.getBonificacion().getObservaciones()) : null)")
    InscripcionResponse toDTO(Inscripcion inscripcion);
}
