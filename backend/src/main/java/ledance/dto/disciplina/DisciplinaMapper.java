package ledance.dto.disciplina;

import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DisciplinaHorarioMapper.class})
public interface DisciplinaMapper {

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
    DisciplinaResponse toResponse(Disciplina disciplina);

    @Mapping(target = "salon.id", source = "salonId")
    Disciplina toEntity(DisciplinaRegistroRequest request);

    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "horarios", ignore = true)
    void updateEntityFromRequest(DisciplinaModificacionRequest request, @org.mapstruct.MappingTarget Disciplina disciplina);

    default String mapSalonToString(Salon salon) {
        if (salon == null) {
            return null;
        }
        // Ajusta segun la propiedad que quieras usar
        // Por ejemplo, si tu Salon tiene un campo "nombre" o "descripcion":
        return salon.getNombre();
    }

}
