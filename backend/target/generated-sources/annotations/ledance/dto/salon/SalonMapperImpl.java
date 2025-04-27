package ledance.dto.salon;

import javax.annotation.processing.Generated;
import ledance.dto.salon.request.SalonModificacionRequest;
import ledance.dto.salon.request.SalonRegistroRequest;
import ledance.dto.salon.response.SalonResponse;
import ledance.entidades.Salon;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:52-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class SalonMapperImpl implements SalonMapper {

    @Override
    public Salon toEntity(SalonRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Salon salon = new Salon();

        salon.setNombre( request.nombre() );
        salon.setDescripcion( request.descripcion() );

        return salon;
    }

    @Override
    public Salon toEntity(SalonModificacionRequest request) {
        if ( request == null ) {
            return null;
        }

        Salon salon = new Salon();

        salon.setNombre( request.nombre() );
        salon.setDescripcion( request.descripcion() );

        return salon;
    }

    @Override
    public SalonResponse toResponse(Salon entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        String descripcion = null;

        id = entity.getId();
        nombre = entity.getNombre();
        descripcion = entity.getDescripcion();

        SalonResponse salonResponse = new SalonResponse( id, nombre, descripcion );

        return salonResponse;
    }
}
