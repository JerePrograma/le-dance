package ledance.dto.mappers;

import ledance.dto.request.DisciplinaRegistroRequest;
import ledance.dto.request.DisciplinaModificacionRequest;
import ledance.dto.response.DisciplinaDetalleResponse;
import ledance.dto.response.DisciplinaListadoResponse;
import ledance.entidades.Disciplina;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DisciplinaMapper {

    /**
     * ✅ Mapea una disciplina a su versión simplificada para listados.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "horarioInicio", source = "horarioInicio")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "profesorNombre", source = "profesor.nombre")
    DisciplinaListadoResponse toListadoResponse(Disciplina disciplina);

    /**
     * ✅ Mapea una disciplina con todos los detalles.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "horarioInicio", source = "horarioInicio")
    @Mapping(target = "duracion", source = "duracion")
    @Mapping(target = "salon", source = "salon.nombre")
    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "valorCuota", source = "valorCuota")
    @Mapping(target = "matricula", source = "matricula")
    @Mapping(target = "profesorNombre", source = "profesor.nombre")
    @Mapping(target = "profesorApellido", source = "profesor.apellido")
    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "inscritos", expression = "java(disciplina.getInscripciones() != null ? disciplina.getInscripciones().size() : 0)")
    @Mapping(target = "activo", source = "activo")
    DisciplinaDetalleResponse toDetalleResponse(Disciplina disciplina);

    /**
     * ✅ Convierte un `DisciplinaRegistroRequest` en una entidad `Disciplina`.
     */
    /**
     * ✅ Convierte un `DisciplinaRegistroRequest` en una entidad `Disciplina`.
     */
    @Mapping(target = "salon.id", source = "salonId")
    Disciplina toEntity(DisciplinaRegistroRequest request);

    @Mapping(target = "salon.id", source = "salonId")
    void updateEntityFromRequest(DisciplinaModificacionRequest request, @MappingTarget Disciplina disciplina);
}
