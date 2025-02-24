package ledance.dto.concepto;

import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.request.SubConceptoModificacionRequest;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.SubConcepto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SubConceptoMapper {
    SubConceptoMapper INSTANCE = Mappers.getMapper(SubConceptoMapper.class);

    @Mapping(target = "descripcion", expression = "java(request.descripcion().toUpperCase())")
    SubConcepto toEntity(SubConceptoRegistroRequest request);

    // Aseguramos que la descripción se convierta a mayúsculas al mapear a respuesta
    @Mapping(target = "descripcion", expression = "java(subConcepto.getDescripcion().toUpperCase())")
    SubConceptoResponse toResponse(SubConcepto subConcepto);

    @Mapping(target = "descripcion", expression = "java(request.descripcion().toUpperCase())")
    void updateEntityFromRequest(SubConceptoModificacionRequest request, @MappingTarget SubConcepto subConcepto);
}
