package ledance.dto.disciplina;

import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.entidades.Disciplina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DisciplinaHorarioMapper.class})
public interface DisciplinaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "profesorNombre", source = "profesor.nombre")
    @Mapping(target = "claseSuelta", source = "claseSuelta")
    @Mapping(target = "clasePrueba", source = "clasePrueba")
    @Mapping(target = "valorCuota", source = "valorCuota")
    DisciplinaListadoResponse toListadoResponse(Disciplina disciplina);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "salon", source = "salon.nombre")
    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "valorCuota", source = "valorCuota")
    @Mapping(target = "profesorNombre", source = "profesor.nombre")
    @Mapping(target = "profesorApellido", source = "profesor.apellido")
    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "inscritos", expression = "java(disciplina.getInscripciones() != null ? disciplina.getInscripciones().size() : 0)")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "horarios", source = "horarios", qualifiedByName = "toResponseList")
    DisciplinaDetalleResponse toDetalleResponse(Disciplina disciplina);

    @Mapping(target = "salon.id", source = "salonId")
    Disciplina toEntity(DisciplinaRegistroRequest request);

    @Mapping(target = "salon.id", source = "salonId")
    @Mapping(target = "horarios", ignore = true)  // <-- Ignoramos el mapeo de horarios en la actualizacion
    void updateEntityFromRequest(DisciplinaModificacionRequest request, @org.mapstruct.MappingTarget Disciplina disciplina);
}
