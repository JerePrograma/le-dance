package ledance.dto.caja;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.caja.request.CajaModificacionRequest;
import ledance.dto.caja.request.CajaRegistroRequest;
import ledance.dto.caja.response.CajaResponse;
import ledance.entidades.Caja;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class CajaMapperImpl implements CajaMapper {

    @Override
    public Caja toEntity(CajaRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Caja caja = new Caja();

        caja.setFecha( request.fecha() );
        caja.setTotalEfectivo( request.totalEfectivo() );
        caja.setTotalTransferencia( request.totalTransferencia() );
        caja.setTotalTarjeta( request.totalTarjeta() );
        caja.setRangoDesdeHasta( request.rangoDesdeHasta() );
        caja.setObservaciones( request.observaciones() );

        caja.setActivo( true );

        return caja;
    }

    @Override
    public CajaResponse toDTO(Caja caja) {
        if ( caja == null ) {
            return null;
        }

        Long id = null;
        LocalDate fecha = null;
        Double totalEfectivo = null;
        Double totalTransferencia = null;
        Double totalTarjeta = null;
        String rangoDesdeHasta = null;
        String observaciones = null;
        Boolean activo = null;

        id = caja.getId();
        fecha = caja.getFecha();
        totalEfectivo = caja.getTotalEfectivo();
        totalTransferencia = caja.getTotalTransferencia();
        totalTarjeta = caja.getTotalTarjeta();
        rangoDesdeHasta = caja.getRangoDesdeHasta();
        observaciones = caja.getObservaciones();
        activo = caja.getActivo();

        CajaResponse cajaResponse = new CajaResponse( id, fecha, totalEfectivo, totalTransferencia, totalTarjeta, rangoDesdeHasta, observaciones, activo );

        return cajaResponse;
    }

    @Override
    public void updateEntityFromRequest(CajaModificacionRequest request, Caja caja) {
        if ( request == null ) {
            return;
        }

        caja.setFecha( request.fecha() );
        caja.setTotalEfectivo( request.totalEfectivo() );
        caja.setTotalTransferencia( request.totalTransferencia() );
        caja.setTotalTarjeta( request.totalTarjeta() );
        caja.setRangoDesdeHasta( request.rangoDesdeHasta() );
        caja.setObservaciones( request.observaciones() );
        caja.setActivo( request.activo() );
    }
}
