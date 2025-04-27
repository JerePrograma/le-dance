package ledance.dto.rol;

import javax.annotation.processing.Generated;
import ledance.dto.rol.request.RolModificacionRequest;
import ledance.dto.rol.request.RolRegistroRequest;
import ledance.dto.rol.response.RolResponse;
import ledance.entidades.Rol;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class RolMapperImpl implements RolMapper {

    @Override
    public Rol toEntity(RolRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Rol rol = new Rol();

        rol.setDescripcion( request.descripcion() );

        rol.setActivo( true );

        return rol;
    }

    @Override
    public RolResponse toDTO(Rol rol) {
        if ( rol == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        Boolean activo = null;

        id = rol.getId();
        descripcion = rol.getDescripcion();
        activo = rol.getActivo();

        RolResponse rolResponse = new RolResponse( id, descripcion, activo );

        return rolResponse;
    }

    @Override
    public void updateEntityFromRequest(RolModificacionRequest request, Rol rol) {
        if ( request == null ) {
            return;
        }

        rol.setDescripcion( request.descripcion() );
        rol.setActivo( request.activo() );
    }
}
