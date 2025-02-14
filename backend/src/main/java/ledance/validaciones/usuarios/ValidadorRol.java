package ledance.validaciones.usuarios;
import ledance.dto.usuario.request.UsuarioRegistroRequest;
import ledance.repositorios.RolRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorRol implements Validador<UsuarioRegistroRequest> {

    private final RolRepositorio rolRepositorio;

    public ValidadorRol(RolRepositorio rolRepositorio) {
        this.rolRepositorio = rolRepositorio;
    }

    @Override
    public void validar(UsuarioRegistroRequest request) {
        boolean existeRol = rolRepositorio.findByDescripcion(request.rol().toUpperCase()).isPresent();
        if (!existeRol) {
            throw new RuntimeException("El rol proporcionado no es valido: " + request.rol());
        }
    }
}
