package ledance.dto.mappers;

import ledance.dto.request.InscripcionRegistroRequest;
import ledance.dto.request.InscripcionModificacionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(target = "alumno", source = "alumno")
    @Mapping(target = "disciplina", source = "disciplina")
    InscripcionResponse toDTO(Inscripcion inscripcion);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellido", source = "apellido")
    InscripcionResponse.AlumnoResumen mapAlumnoResumen(Alumno alumno);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "profesorNombre", source = "profesor.nombre")
    InscripcionResponse.DisciplinaResumen mapDisciplinaResumen(Disciplina disciplina);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "estado", constant = "ACTIVA")
    Inscripcion toEntity(InscripcionRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    void updateEntityFromRequest(InscripcionModificacionRequest request, @MappingTarget Inscripcion inscripcion);
}