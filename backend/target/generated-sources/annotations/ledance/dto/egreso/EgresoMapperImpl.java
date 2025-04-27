package ledance.dto.egreso;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.entidades.Egreso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class EgresoMapperImpl implements EgresoMapper {

    @Autowired
    private MetodoPagoMapper metodoPagoMapper;

    @Override
    public Egreso toEntity(EgresoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Egreso egreso = new Egreso();

        egreso.setFecha( request.fecha() );
        egreso.setMonto( request.monto() );
        egreso.setObservaciones( request.observaciones() );

        return egreso;
    }

    @Override
    public EgresoResponse toDTO(Egreso egreso) {
        if ( egreso == null ) {
            return null;
        }

        MetodoPagoResponse metodoPago = null;
        Long id = null;
        LocalDate fecha = null;
        Double monto = null;
        String observaciones = null;
        Boolean activo = null;

        metodoPago = metodoPagoMapper.toDTO( egreso.getMetodoPago() );
        id = egreso.getId();
        fecha = egreso.getFecha();
        monto = egreso.getMonto();
        observaciones = egreso.getObservaciones();
        activo = egreso.getActivo();

        EgresoResponse egresoResponse = new EgresoResponse( id, fecha, monto, observaciones, metodoPago, activo );

        return egresoResponse;
    }

    @Override
    public void updateEntityFromRequest(EgresoRegistroRequest request, Egreso egreso) {
        if ( request == null ) {
            return;
        }

        egreso.setFecha( request.fecha() );
        egreso.setMonto( request.monto() );
        egreso.setObservaciones( request.observaciones() );
    }
}
