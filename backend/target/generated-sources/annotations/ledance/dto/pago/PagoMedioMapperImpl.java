package ledance.dto.pago;

import javax.annotation.processing.Generated;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.response.PagoMedioResponse;
import ledance.entidades.MetodoPago;
import ledance.entidades.PagoMedio;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class PagoMedioMapperImpl implements PagoMedioMapper {

    @Override
    public PagoMedio toEntity(PagoMedioRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        PagoMedio pagoMedio = new PagoMedio();

        return pagoMedio;
    }

    @Override
    public PagoMedioResponse toDTO(PagoMedio pagoMedio) {
        if ( pagoMedio == null ) {
            return null;
        }

        Long metodoPagoId = null;
        String metodoPagoDescripcion = null;
        Long id = null;
        Double monto = null;

        metodoPagoId = pagoMedioMetodoId( pagoMedio );
        metodoPagoDescripcion = pagoMedioMetodoDescripcion( pagoMedio );
        id = pagoMedio.getId();
        monto = pagoMedio.getMonto();

        PagoMedioResponse pagoMedioResponse = new PagoMedioResponse( id, monto, metodoPagoId, metodoPagoDescripcion );

        return pagoMedioResponse;
    }

    private Long pagoMedioMetodoId(PagoMedio pagoMedio) {
        MetodoPago metodo = pagoMedio.getMetodo();
        if ( metodo == null ) {
            return null;
        }
        return metodo.getId();
    }

    private String pagoMedioMetodoDescripcion(PagoMedio pagoMedio) {
        MetodoPago metodo = pagoMedio.getMetodo();
        if ( metodo == null ) {
            return null;
        }
        return metodo.getDescripcion();
    }
}
