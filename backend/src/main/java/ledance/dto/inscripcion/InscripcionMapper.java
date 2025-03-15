package ledance.dto.inscripcion;

import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.Bonificacion;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(source = "alumno", target = "alumno")
    @Mapping(source = "disciplina", target = "disciplina")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", source = "estado")
        // Se elimina la asignacion de costoCalculado; se calculará desde el servicio.
    InscripcionResponse toDTO(Inscripcion inscripcion);

    // Para crear una inscripcion a partir del nuevo DTO de registro
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    // Asigna la disciplina a partir del id contenido en request.disciplina
    @Mapping(target = "disciplina", expression = "java(mapDisciplina(request.disciplina().id()))")
    // Para la bonificacion, si se envía un id se mapea; de lo contrario, se asigna null
    @Mapping(target = "bonificacion", expression = "java(request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null)")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", constant = "ACTIVA")
    Inscripcion toEntity(InscripcionRegistroRequest request);

    // Para actualizar una inscripcion a partir del DTO de modificacion
    @Mapping(target = "alumno", ignore = true)
    // Se extrae la disciplina directamente desde request.disciplina(), ya que el DTO ya lo posee
    @Mapping(target = "disciplina", expression = "java(mapDisciplina(request.disciplina().id()))")
    @Mapping(target = "bonificacion", expression = "java(request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null)")
    @Mapping(target = "fechaBaja", source = "fechaBaja")
    Inscripcion updateEntityFromRequest(InscripcionModificacionRequest request, @MappingTarget Inscripcion inscripcion);

    // Métodos auxiliares para crear instancias mínimas de Disciplina y Bonificacion a partir del id
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
