package ledance.dto.recargo;

import javax.annotation.processing.Generated;
import ledance.dto.recargo.request.RecargoRegistroRequest;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.entidades.Recargo;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class RecargoMapperImpl implements RecargoMapper {

    @Override
    public Recargo toEntity(RecargoRegistroRequest dto) {
        if ( dto == null ) {
            return null;
        }

        Recargo recargo = new Recargo();

        recargo.setDescripcion( dto.descripcion() );
        recargo.setPorcentaje( dto.porcentaje() );
        recargo.setValorFijo( dto.valorFijo() );
        recargo.setDiaDelMesAplicacion( dto.diaDelMesAplicacion() );

        return recargo;
    }

    @Override
    public RecargoResponse toResponse(Recargo recargo) {
        if ( recargo == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        Double porcentaje = null;
        Double valorFijo = null;
        Integer diaDelMesAplicacion = null;

        id = recargo.getId();
        descripcion = recargo.getDescripcion();
        porcentaje = recargo.getPorcentaje();
        valorFijo = recargo.getValorFijo();
        diaDelMesAplicacion = recargo.getDiaDelMesAplicacion();

        RecargoResponse recargoResponse = new RecargoResponse( id, descripcion, porcentaje, valorFijo, diaDelMesAplicacion );

        return recargoResponse;
    }
}
