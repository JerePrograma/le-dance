package ledance.dto.pago;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.Alumno;
import ledance.entidades.DetallePago;
import ledance.entidades.TipoDetallePago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class DetallePagoMapperImpl implements DetallePagoMapper {

    @Autowired
    private AlumnoMapper alumnoMapper;

    @Override
    public DetallePago toEntity(DetallePagoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        DetallePago detallePago = new DetallePago();

        detallePago.setAlumno( alumnoMapper.toEntity( request.alumno() ) );
        detallePago.setMensualidad( mapMensualidad( request.mensualidadId() ) );
        detallePago.setMatricula( mapMatricula( request.matriculaId() ) );
        detallePago.setStock( mapStock( request.stockId() ) );
        detallePago.setPago( mapPago( request.pagoId() ) );
        detallePago.setConcepto( mapConcepto( request.conceptoId() ) );
        detallePago.setSubConcepto( mapSubConcepto( request.subConceptoId() ) );
        detallePago.setValorBase( request.valorBase() );
        detallePago.setCuotaOCantidad( request.cuotaOCantidad() );
        detallePago.setACobrar( request.ACobrar() );
        detallePago.setTieneRecargo( request.tieneRecargo() );
        detallePago.setImporteInicial( request.importeInicial() );
        detallePago.setImportePendiente( request.importePendiente() );
        detallePago.setCobrado( request.cobrado() );
        detallePago.setRemovido( request.removido() );

        detallePago.setId( (request.id() != null && request.id() == 0) ? null : request.id() );
        detallePago.setDescripcionConcepto( request.descripcionConcepto() != null ? request.descripcionConcepto().trim().toUpperCase() : null );
        detallePago.setTipo( determineTipo(request) );

        return detallePago;
    }

    @Override
    public void updateDetallePagoFromDTO(DetallePagoRegistroRequest request, DetallePago detallePago) {
        if ( request == null ) {
            return;
        }

        if ( request.alumno() != null ) {
            if ( detallePago.getAlumno() == null ) {
                detallePago.setAlumno( new Alumno() );
            }
            alumnoMapper.updateEntityFromRequest( request.alumno(), detallePago.getAlumno() );
        }
        else {
            detallePago.setAlumno( null );
        }
        detallePago.setTieneRecargo( request.tieneRecargo() );
        detallePago.setMensualidad( mapMensualidad( request.mensualidadId() ) );
        detallePago.setMatricula( mapMatricula( request.matriculaId() ) );
        detallePago.setStock( mapStock( request.stockId() ) );
        detallePago.setValorBase( request.valorBase() );
        detallePago.setCuotaOCantidad( request.cuotaOCantidad() );
        detallePago.setImporteInicial( request.importeInicial() );
        detallePago.setImportePendiente( request.importePendiente() );
        detallePago.setACobrar( request.ACobrar() );
        detallePago.setCobrado( request.cobrado() );
        detallePago.setRemovido( request.removido() );

        detallePago.setId( (request.id() != null && request.id() == 0) ? null : request.id() );
        detallePago.setDescripcionConcepto( request.descripcionConcepto() != null ? request.descripcionConcepto().trim().toUpperCase() : null );
        detallePago.setTipo( determineTipo(request) );
    }

    @Override
    public List<DetallePago> toEntity(List<DetallePagoRegistroRequest> requests) {
        if ( requests == null ) {
            return null;
        }

        List<DetallePago> list = new ArrayList<DetallePago>( requests.size() );
        for ( DetallePagoRegistroRequest detallePagoRegistroRequest : requests ) {
            list.add( toEntity( detallePagoRegistroRequest ) );
        }

        return list;
    }

    @Override
    public DetallePagoResponse toDTO(DetallePago detallePago) {
        if ( detallePago == null ) {
            return null;
        }

        Long id = null;
        Long version = null;
        String descripcionConcepto = null;
        String cuotaOCantidad = null;
        Double valorBase = null;
        Double importeInicial = null;
        Double importePendiente = null;
        Double aCobrar = null;
        Boolean cobrado = null;
        TipoDetallePago tipo = null;
        LocalDate fechaRegistro = null;
        AlumnoListadoResponse alumno = null;
        Boolean tieneRecargo = null;
        String estadoPago = null;

        id = detallePago.getId();
        version = detallePago.getVersion();
        descripcionConcepto = detallePago.getDescripcionConcepto();
        cuotaOCantidad = detallePago.getCuotaOCantidad();
        valorBase = detallePago.getValorBase();
        importeInicial = detallePago.getImporteInicial();
        importePendiente = detallePago.getImportePendiente();
        aCobrar = detallePago.getACobrar();
        cobrado = detallePago.getCobrado();
        tipo = detallePago.getTipo();
        fechaRegistro = detallePago.getFechaRegistro();
        alumno = alumnoMapper.toAlumnoListadoResponse( detallePago.getAlumno() );
        tieneRecargo = detallePago.getTieneRecargo();
        if ( detallePago.getEstadoPago() != null ) {
            estadoPago = detallePago.getEstadoPago().name();
        }

        Long conceptoId = detallePago.getConcepto() != null ? detallePago.getConcepto().getId() : null;
        Long subConceptoId = detallePago.getSubConcepto() != null ? detallePago.getSubConcepto().getId() : null;
        Long bonificacionId = detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null;
        String bonificacionNombre = detallePago.getBonificacion() != null ? detallePago.getBonificacion().getDescripcion() : "";
        Long recargoId = detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null;
        Long mensualidadId = detallePago.getMensualidad() != null ? detallePago.getMensualidad().getId() : null;
        Long matriculaId = detallePago.getMatricula() != null ? detallePago.getMatricula().getId() : null;
        Long stockId = detallePago.getStock() != null ? detallePago.getStock().getId() : null;
        Long pagoId = detallePago.getPago() != null ? detallePago.getPago().getId() : null;
        Long usuarioId = null;

        DetallePagoResponse detallePagoResponse = new DetallePagoResponse( id, version, descripcionConcepto, cuotaOCantidad, valorBase, bonificacionId, bonificacionNombre, recargoId, aCobrar, cobrado, conceptoId, subConceptoId, mensualidadId, matriculaId, stockId, importeInicial, importePendiente, tipo, fechaRegistro, pagoId, alumno, tieneRecargo, usuarioId, estadoPago );

        return detallePagoResponse;
    }
}
