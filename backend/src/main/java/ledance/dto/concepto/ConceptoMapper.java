package ledance.dto.concepto;

import ledance.dto.concepto.request.ConceptoRegistroRequest;
import ledance.dto.concepto.request.ConceptoModificacionRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.entidades.Concepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {SubConceptoMapper.class})
public interface ConceptoMapper {
    ConceptoMapper INSTANCE = Mappers.getMapper(ConceptoMapper.class);

    // Mapea de registro a entidad; se ignora la asociación subConcepto, que se asignará en el servicio.
    @Mapping(target = "subConcepto", ignore = true)
    Concepto toEntity(ConceptoRegistroRequest request);

    ConceptoResponse toResponse(Concepto concepto);

    void updateEntityFromRequest(ConceptoModificacionRequest request, @MappingTarget Concepto concepto);
}
