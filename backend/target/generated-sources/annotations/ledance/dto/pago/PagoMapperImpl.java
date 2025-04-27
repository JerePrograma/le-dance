package ledance.dto.pago;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoMedioResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Alumno;
import ledance.entidades.DetallePago;
import ledance.entidades.MetodoPago;
import ledance.entidades.Pago;
import ledance.entidades.PagoMedio;
import ledance.entidades.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:50-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class PagoMapperImpl implements PagoMapper {

    @Autowired
    private AlumnoMapper alumnoMapper;
    @Autowired
    private DetallePagoMapper detallePagoMapper;

    @Override
    public Pago toEntity(PagoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Pago pago = new Pago();

        pago.setDetallePagos( detallePagoMapper.toEntity( request.detallePagos() ) );
        pago.setFecha( request.fecha() );
        pago.setFechaVencimiento( request.fechaVencimiento() );
        pago.setMonto( request.monto() );
        pago.setValorBase( request.valorBase() );
        pago.setImporteInicial( request.importeInicial() );
        pago.setAlumno( alumnoMapper.toEntity( request.alumno() ) );

        return pago;
    }

    @Override
    public PagoResponse toDTO(Pago pago) {
        if ( pago == null ) {
            return null;
        }

        Long usuarioId = null;
        Long id = null;
        LocalDate fecha = null;
        LocalDate fechaVencimiento = null;
        Double monto = null;
        Double valorBase = null;
        Double importeInicial = null;
        Double montoPagado = null;
        Double saldoRestante = null;
        String estadoPago = null;
        MetodoPagoResponse metodoPago = null;
        String observaciones = null;
        List<DetallePagoResponse> detallePagos = null;
        List<PagoMedioResponse> pagoMedios = null;

        usuarioId = pagoUsuarioId( pago );
        id = pago.getId();
        fecha = pago.getFecha();
        fechaVencimiento = pago.getFechaVencimiento();
        monto = pago.getMonto();
        valorBase = pago.getValorBase();
        importeInicial = pago.getImporteInicial();
        montoPagado = pago.getMontoPagado();
        saldoRestante = pago.getSaldoRestante();
        if ( pago.getEstadoPago() != null ) {
            estadoPago = pago.getEstadoPago().name();
        }
        metodoPago = metodoPagoToMetodoPagoResponse( pago.getMetodoPago() );
        observaciones = pago.getObservaciones();
        detallePagos = detallePagoListToDetallePagoResponseList( pago.getDetallePagos() );
        pagoMedios = pagoMedioListToPagoMedioResponseList( pago.getPagoMedios() );

        AlumnoResponse alumno = alumnoMapper.toResponse(pago.getAlumno());

        PagoResponse pagoResponse = new PagoResponse( id, fecha, fechaVencimiento, monto, valorBase, importeInicial, montoPagado, saldoRestante, estadoPago, alumno, metodoPago, observaciones, detallePagos, pagoMedios, usuarioId );

        return pagoResponse;
    }

    @Override
    public void updateEntityFromRequest(PagoRegistroRequest request, Pago pago) {
        if ( request == null ) {
            return;
        }

        pago.setId( request.id() );
        pago.setFecha( request.fecha() );
        pago.setFechaVencimiento( request.fechaVencimiento() );
        pago.setMonto( request.monto() );
        pago.setValorBase( request.valorBase() );
        pago.setImporteInicial( request.importeInicial() );
        if ( request.alumno() != null ) {
            if ( pago.getAlumno() == null ) {
                pago.setAlumno( new Alumno() );
            }
            alumnoMapper.updateEntityFromRequest( request.alumno(), pago.getAlumno() );
        }
        else {
            pago.setAlumno( null );
        }
    }

    private Long pagoUsuarioId(Pago pago) {
        Usuario usuario = pago.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        return usuario.getId();
    }

    protected MetodoPagoResponse metodoPagoToMetodoPagoResponse(MetodoPago metodoPago) {
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

    protected List<DetallePagoResponse> detallePagoListToDetallePagoResponseList(List<DetallePago> list) {
        if ( list == null ) {
            return null;
        }

        List<DetallePagoResponse> list1 = new ArrayList<DetallePagoResponse>( list.size() );
        for ( DetallePago detallePago : list ) {
            list1.add( detallePagoMapper.toDTO( detallePago ) );
        }

        return list1;
    }

    protected PagoMedioResponse pagoMedioToPagoMedioResponse(PagoMedio pagoMedio) {
        if ( pagoMedio == null ) {
            return null;
        }

        Long id = null;
        Double monto = null;

        id = pagoMedio.getId();
        monto = pagoMedio.getMonto();

        Long metodoPagoId = null;
        String metodoPagoDescripcion = null;

        PagoMedioResponse pagoMedioResponse = new PagoMedioResponse( id, monto, metodoPagoId, metodoPagoDescripcion );

        return pagoMedioResponse;
    }

    protected List<PagoMedioResponse> pagoMedioListToPagoMedioResponseList(List<PagoMedio> list) {
        if ( list == null ) {
            return null;
        }

        List<PagoMedioResponse> list1 = new ArrayList<PagoMedioResponse>( list.size() );
        for ( PagoMedio pagoMedio : list ) {
            list1.add( pagoMedioToPagoMedioResponse( pagoMedio ) );
        }

        return list1;
    }
}
