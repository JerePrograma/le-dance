package ledance.dto.concepto;

import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.SubConcepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SubConceptoMapper {

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    SubConcepto toEntity(SubConceptoRegistroRequest request);

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    SubConceptoResponse toResponse(SubConcepto subConcepto);

    @Mapping(target = "descripcion", source = "descripcion", qualifiedByName = "toUpperCase")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void updateEntityFromRequest(SubConceptoRegistroRequest request, @MappingTarget SubConcepto subConcepto);

    @Named("toUpperCase")
    default String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
}
