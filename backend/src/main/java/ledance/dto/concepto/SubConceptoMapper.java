package ledance.dto.concepto;

import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.request.SubConceptoModificacionRequest;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.SubConcepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SubConceptoMapper {

    SubConceptoMapper INSTANCE = Mappers.getMapper(SubConceptoMapper.class);

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    SubConcepto toEntity(SubConceptoRegistroRequest request);

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    SubConceptoResponse toResponse(SubConcepto subConcepto);

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    void updateEntityFromRequest(SubConceptoModificacionRequest request, @MappingTarget SubConcepto subConcepto);

    @Named("toUpperCase")
    default String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
}
