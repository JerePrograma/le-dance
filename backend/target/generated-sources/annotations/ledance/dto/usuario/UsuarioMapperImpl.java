package ledance.dto.usuario;

import javax.annotation.processing.Generated;
import ledance.dto.usuario.request.UsuarioModificacionRequest;
import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.entidades.Usuario;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public Usuario toEntity(UsuarioRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Usuario usuario = new Usuario();

        usuario.setNombreUsuario( request.nombreUsuario() );

        usuario.setActivo( true );

        return usuario;
    }

    @Override
    public UsuarioResponse toDTO(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        Long id = null;
        String nombreUsuario = null;
        Boolean activo = null;

        id = usuario.getId();
        nombreUsuario = usuario.getNombreUsuario();
        activo = usuario.getActivo();

        String rol = usuario.getRol() != null ? usuario.getRol().getDescripcion() : null;

        UsuarioResponse usuarioResponse = new UsuarioResponse( id, nombreUsuario, rol, activo );

        return usuarioResponse;
    }

    @Override
    public void updateEntityFromRequest(UsuarioModificacionRequest request, Usuario usuario) {
        if ( request == null ) {
            return;
        }

        usuario.setNombreUsuario( request.nombreUsuario() );
        usuario.setActivo( request.activo() );
    }
}
