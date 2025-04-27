package ledance.dto.bonificacion;

import javax.annotation.processing.Generated;
import ledance.dto.bonificacion.request.BonificacionModificacionRequest;
import ledance.dto.bonificacion.request.BonificacionRegistroRequest;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:52-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class BonificacionMapperImpl implements BonificacionMapper {

    @Override
    public Bonificacion toEntity(BonificacionRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Bonificacion bonificacion = new Bonificacion();

        bonificacion.setDescripcion( request.descripcion() );
        bonificacion.setPorcentajeDescuento( request.porcentajeDescuento() );
        bonificacion.setValorFijo( request.valorFijo() );
        bonificacion.setObservaciones( request.observaciones() );

        bonificacion.setActivo( true );

        return bonificacion;
    }

    @Override
    public BonificacionResponse toDTO(Bonificacion bonificacion) {
        if ( bonificacion == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        Integer porcentajeDescuento = null;
        Boolean activo = null;
        String observaciones = null;
        Double valorFijo = null;

        id = bonificacion.getId();
        descripcion = bonificacion.getDescripcion();
        porcentajeDescuento = bonificacion.getPorcentajeDescuento();
        activo = bonificacion.getActivo();
        observaciones = bonificacion.getObservaciones();
        valorFijo = bonificacion.getValorFijo();

        BonificacionResponse bonificacionResponse = new BonificacionResponse( id, descripcion, porcentajeDescuento, activo, observaciones, valorFijo );

        return bonificacionResponse;
    }

    @Override
    public void updateEntityFromRequest(BonificacionModificacionRequest request, Bonificacion bonificacion) {
        if ( request == null ) {
            return;
        }

        bonificacion.setDescripcion( request.descripcion() );
        bonificacion.setPorcentajeDescuento( request.porcentajeDescuento() );
        bonificacion.setValorFijo( request.valorFijo() );
        bonificacion.setActivo( request.activo() );
        bonificacion.setObservaciones( request.observaciones() );
    }
}
