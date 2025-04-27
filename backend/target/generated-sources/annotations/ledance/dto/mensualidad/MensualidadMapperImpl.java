package ledance.dto.mensualidad;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.Bonificacion;
import ledance.entidades.EstadoMensualidad;
import ledance.entidades.Inscripcion;
import ledance.entidades.Mensualidad;
import ledance.entidades.Recargo;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:52-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class MensualidadMapperImpl implements MensualidadMapper {

    @Override
    public MensualidadResponse toDTO(Mensualidad mensualidad) {
        if ( mensualidad == null ) {
            return null;
        }

        Long recargoId = null;
        BonificacionResponse bonificacion = null;
        Long inscripcionId = null;
        Double importeInicial = null;
        Long id = null;
        LocalDate fechaCuota = null;
        Double valorBase = null;
        String estado = null;
        String descripcion = null;
        Double importePendiente = null;

        recargoId = mensualidadRecargoId( mensualidad );
        bonificacion = bonificacionToBonificacionResponse( mensualidad.getBonificacion() );
        inscripcionId = mensualidadInscripcionId( mensualidad );
        importeInicial = mensualidad.getImporteInicial();
        id = mensualidad.getId();
        fechaCuota = mensualidad.getFechaCuota();
        valorBase = mensualidad.getValorBase();
        if ( mensualidad.getEstado() != null ) {
            estado = mensualidad.getEstado().name();
        }
        descripcion = mensualidad.getDescripcion();
        importePendiente = mensualidad.getImportePendiente();

        MensualidadResponse mensualidadResponse = new MensualidadResponse( id, fechaCuota, valorBase, recargoId, bonificacion, estado, inscripcionId, importeInicial, descripcion, importePendiente );

        return mensualidadResponse;
    }

    @Override
    public void updateEntityFromRequest(MensualidadRegistroRequest dto, Mensualidad mensualidad) {
        if ( dto == null ) {
            return;
        }

        mensualidad.setFechaCuota( dto.fechaCuota() );
        mensualidad.setValorBase( dto.valorBase() );
        if ( dto.estado() != null ) {
            mensualidad.setEstado( Enum.valueOf( EstadoMensualidad.class, dto.estado() ) );
        }
        else {
            mensualidad.setEstado( null );
        }
    }

    private Long mensualidadRecargoId(Mensualidad mensualidad) {
        Recargo recargo = mensualidad.getRecargo();
        if ( recargo == null ) {
            return null;
        }
        return recargo.getId();
    }

    protected BonificacionResponse bonificacionToBonificacionResponse(Bonificacion bonificacion) {
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

    private Long mensualidadInscripcionId(Mensualidad mensualidad) {
        Inscripcion inscripcion = mensualidad.getInscripcion();
        if ( inscripcion == null ) {
            return null;
        }
        return inscripcion.getId();
    }
}
