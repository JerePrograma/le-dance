package ledance.dto.metodopago;

import javax.annotation.processing.Generated;
import ledance.dto.metodopago.request.MetodoPagoRegistroRequest;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.entidades.MetodoPago;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class MetodoPagoMapperImpl implements MetodoPagoMapper {

    @Override
    public MetodoPago toEntity(MetodoPagoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        MetodoPago metodoPago = new MetodoPago();

        metodoPago.setDescripcion( request.descripcion() );
        metodoPago.setActivo( request.activo() );
        metodoPago.setRecargo( request.recargo() );

        return metodoPago;
    }

    @Override
    public MetodoPagoResponse toDTO(MetodoPago metodoPago) {
        if ( metodoPago == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        Boolean activo = null;
        Double recargo = null;

        id = metodoPago.getId();
        descripcion = metodoPago.getDescripcion();
        activo = metodoPago.getActivo();
        recargo = metodoPago.getRecargo();

        MetodoPagoResponse metodoPagoResponse = new MetodoPagoResponse( id, descripcion, activo, recargo );

        return metodoPagoResponse;
    }

    @Override
    public void updateEntityFromRequest(MetodoPagoRegistroRequest request, MetodoPago metodoPago) {
        if ( request == null ) {
            return;
        }

        metodoPago.setDescripcion( request.descripcion() );
        metodoPago.setActivo( request.activo() );
        metodoPago.setRecargo( request.recargo() );
    }
}
