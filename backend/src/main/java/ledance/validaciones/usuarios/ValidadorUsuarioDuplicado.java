package ledance.validaciones.usuarios;

import ledance.dto.request.UsuarioRegistroRequest;
import ledance.repositorios.UsuarioRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorUsuarioDuplicado implements Validador<UsuarioRegistroRequest> {

    private final UsuarioRepositorio usuarioRepositorio;

    public ValidadorUsuarioDuplicado(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public void validar(UsuarioRegistroRequest datos) {
        if (usuarioRepositorio.findByEmail(datos.email()).isPresent()) {
            throw new RuntimeException("El email ya esta registrado: " + datos.email());
        }
    }
}
