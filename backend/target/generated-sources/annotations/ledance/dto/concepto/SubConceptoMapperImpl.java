package ledance.dto.concepto;

import javax.annotation.processing.Generated;
import ledance.dto.concepto.request.SubConceptoRegistroRequest;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.SubConcepto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class SubConceptoMapperImpl implements SubConceptoMapper {

    @Override
    public SubConcepto toEntity(SubConceptoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        SubConcepto subConcepto = new SubConcepto();

        subConcepto.setDescripcion( toUpperCase( request.descripcion() ) );
        subConcepto.setId( request.id() );

        return subConcepto;
    }

    @Override
    public SubConceptoResponse toResponse(SubConcepto subConcepto) {
        if ( subConcepto == null ) {
            return null;
        }

        String descripcion = null;
        Long id = null;

        descripcion = toUpperCase( subConcepto.getDescripcion() );
        id = subConcepto.getId();

        SubConceptoResponse subConceptoResponse = new SubConceptoResponse( id, descripcion );

        return subConceptoResponse;
    }

    @Override
    public void updateEntityFromRequest(SubConceptoRegistroRequest request, SubConcepto subConcepto) {
        if ( request == null ) {
            return;
        }

        subConcepto.setDescripcion( toUpperCase( request.descripcion() ) );
        subConcepto.setId( request.id() );
    }
}
