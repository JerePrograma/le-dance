package ledance.dto.mappers;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.dto.response.ProfesorResponse;
import ledance.entidades.Asistencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections; // ✅ Importación añadida

@Mapper(componentModel = "spring", imports = {Collections.class}) // ✅ `imports = Collections.class`
public interface AsistenciaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "profesor", ignore = true)
    Asistencia toEntity(AsistenciaRequest request);

    @Mapping(target = "alumno", expression = "java(new AlumnoListadoResponse(asistencia.getAlumno().getId(), asistencia.getAlumno().getNombre(), asistencia.getAlumno().getApellido(), asistencia.getAlumno().getActivo()))")
    @Mapping(target = "disciplina", expression = "java(new DisciplinaSimpleResponse(asistencia.getDisciplina().getId(), asistencia.getDisciplina().getNombre()))")
    @Mapping(target = "profesor", expression = "java(asistencia.getProfesor() != null ? "
            + "new ProfesorResponse(asistencia.getProfesor().getId(), "
            + "asistencia.getProfesor().getNombre(), "
            + "asistencia.getProfesor().getApellido(), "
            + "asistencia.getProfesor().getEspecialidad(), "
            + "asistencia.getProfesor().getActivo(), "
            + "java.util.Collections.emptyList()) : null)") // ✅ `java.util.Collections.emptyList()`
    AsistenciaResponseDTO toResponseDTO(Asistencia asistencia);
}
