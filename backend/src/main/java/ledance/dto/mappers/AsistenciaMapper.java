package ledance.dto.mappers;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.entidades.Asistencia;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsistenciaMapper {

    // En este caso, no podemos mapear automáticamente las asociaciones
    // ya que se deben obtener de otros servicios o repositorios.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    Asistencia toEntity(AsistenciaRequest request);

    // Para el DTO, usamos expresiones para mapear las asociaciones.
    @Mapping(target = "alumno", expression = "java(new AlumnoListadoResponse(asistencia.getAlumno().getId(), asistencia.getAlumno().getNombre(), asistencia.getAlumno().getApellido()))")
    @Mapping(target = "disciplina", expression = "java(new DisciplinaSimpleResponse(asistencia.getDisciplina().getId(), asistencia.getDisciplina().getNombre()))")
    AsistenciaResponseDTO toResponseDTO(Asistencia asistencia);
}
