package ledance.dto.concepto;

import javax.annotation.processing.Generated;
import ledance.dto.concepto.request.ConceptoRegistroRequest;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.concepto.response.SubConceptoResponse;
import ledance.entidades.Concepto;
import ledance.entidades.SubConcepto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class ConceptoMapperImpl implements ConceptoMapper {

    @Autowired
    private SubConceptoMapper subConceptoMapper;

    @Override
    public Concepto toEntity(ConceptoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Concepto concepto = new Concepto();

        concepto.setDescripcion( request.descripcion() );
        concepto.setPrecio( request.precio() );

        return concepto;
    }

    @Override
    public ConceptoResponse toResponse(Concepto concepto) {
        if ( concepto == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        double precio = 0.0d;
        SubConceptoResponse subConcepto = null;

        id = concepto.getId();
        descripcion = concepto.getDescripcion();
        precio = concepto.getPrecio();
        subConcepto = subConceptoMapper.toResponse( concepto.getSubConcepto() );

        ConceptoResponse conceptoResponse = new ConceptoResponse( id, descripcion, precio, subConcepto );

        return conceptoResponse;
    }

    @Override
    public void updateEntityFromRequest(ConceptoRegistroRequest request, Concepto concepto) {
        if ( request == null ) {
            return;
        }

        concepto.setDescripcion( request.descripcion() );
        concepto.setPrecio( request.precio() );
        if ( request.subConcepto() != null ) {
            if ( concepto.getSubConcepto() == null ) {
                concepto.setSubConcepto( new SubConcepto() );
            }
            subConceptoMapper.updateEntityFromRequest( request.subConcepto(), concepto.getSubConcepto() );
        }
        else {
            concepto.setSubConcepto( null );
        }
    }
}
