package ledance.dto.mappers;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.dto.response.ProfesorResponse;
import ledance.entidades.Asistencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsistenciaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "profesor", ignore = true) // ✅ Ignoramos en el mapeo inicial
    Asistencia toEntity(AsistenciaRequest request);

    @Mapping(target = "alumno", expression = "java(new AlumnoListadoResponse(asistencia.getAlumno().getId(), asistencia.getAlumno().getNombre(), asistencia.getAlumno().getApellido()))")
    @Mapping(target = "disciplina", expression = "java(new DisciplinaSimpleResponse(asistencia.getDisciplina().getId(), asistencia.getDisciplina().getNombre()))")
    @Mapping(target = "profesor", expression = "java(asistencia.getProfesor() != null ? new ProfesorResponse(asistencia.getProfesor().getId(), asistencia.getProfesor().getNombre(), asistencia.getProfesor().getApellido(), asistencia.getProfesor().getEspecialidad()) : null)")
    AsistenciaResponseDTO toResponseDTO(Asistencia asistencia);
}
