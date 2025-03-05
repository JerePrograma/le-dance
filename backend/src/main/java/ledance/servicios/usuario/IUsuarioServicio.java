package ledance.servicios.usuario;

import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.entidades.Rol;

import java.util.List;

public interface IUsuarioServicio {
    String registrarUsuario(UsuarioRegistroRequest request);

    List<Usuario> listarUsuarios(String rolDescripcion, Boolean activo);

    UsuarioResponse convertirAUsuarioResponse(Usuario usuario);
}