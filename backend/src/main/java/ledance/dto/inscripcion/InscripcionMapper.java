package ledance.dto.inscripcion;

import ledance.dto.CommonMapper;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.Bonificacion;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface InscripcionMapper {

    // Método estándar: mapea una inscripción a InscripcionResponse y ya ignora el alumno
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "disciplina", source = "disciplina")
    @Mapping(target = "alumno", source = "alumno") // Rompe la recursividad
    InscripcionResponse toDTO(Inscripcion inscripcion);

    // Versión simple para listas (puedes definirla como default)
    default List<InscripcionResponse> toSimpleDTOList(List<Inscripcion> inscripciones) {
        if (inscripciones == null) {
            return null;
        }
        return inscripciones.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Para crear una inscripción a partir del DTO de registro
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", expression = "java(mapDisciplina(request.disciplina().id()))")
    @Mapping(target = "bonificacion", expression = "java(request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null)")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", constant = "ACTIVA")
    Inscripcion toEntity(InscripcionRegistroRequest request);

    // Para actualizar una inscripción a partir del DTO de modificación
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", expression = "java(mapDisciplina(request.disciplina().id()))")
    @Mapping(target = "bonificacion", expression = "java(request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null)")
    @Mapping(target = "fechaBaja", source = "fechaBaja")
    Inscripcion updateEntityFromRequest(InscripcionRegistroRequest request, @MappingTarget Inscripcion inscripcion);

    // Métodos auxiliares para crear instancias mínimas de Disciplina y Bonificación a partir del id
    default Disciplina mapDisciplina(Long disciplinaId) {
        Disciplina d = new Disciplina();
        d.setId(disciplinaId);
        return d;
    }

    default Bonificacion mapBonificacion(Long bonificacionId) {
        Bonificacion b = new Bonificacion();
        b.setId(bonificacionId);
        return b;
    }
}
