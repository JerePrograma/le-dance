package ledance.dto.reporte;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.reporte.request.ReporteModificacionRequest;
import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.entidades.Reporte;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class ReporteMapperImpl implements ReporteMapper {

    @Override
    public Reporte toEntity(ReporteRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Reporte reporte = new Reporte();

        reporte.setTipo( request.tipo() );
        if ( request.descripcion() != null ) {
            reporte.setDescripcion( request.descripcion() );
        }
        else {
            reporte.setDescripcion( "Sin descripcion" );
        }
        reporte.setFechaGeneracion( request.fechaGeneracion() );

        reporte.setActivo( true );

        return reporte;
    }

    @Override
    public ReporteResponse toDTO(Reporte reporte) {
        if ( reporte == null ) {
            return null;
        }

        Long id = null;
        String tipo = null;
        String descripcion = null;
        LocalDate fechaGeneracion = null;
        Boolean activo = null;

        id = reporte.getId();
        tipo = reporte.getTipo();
        descripcion = reporte.getDescripcion();
        fechaGeneracion = reporte.getFechaGeneracion();
        activo = reporte.getActivo();

        ReporteResponse reporteResponse = new ReporteResponse( id, tipo, descripcion, fechaGeneracion, activo );

        return reporteResponse;
    }

    @Override
    public void updateEntityFromRequest(ReporteModificacionRequest request, Reporte reporte) {
        if ( request == null ) {
            return;
        }

        reporte.setDescripcion( request.descripcion() );
        reporte.setActivo( request.activo() );
    }
}
