package ledance.infra.seguridad;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ledance.repositorios.UsuarioRepositorio;

@Service
public class AutenticacionService implements UserDetailsService {

    private final UsuarioRepositorio repositorio;

    public AutenticacionService(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        // El repositorio retorna Optional<Usuario>
        return repositorio.findByNombreUsuario(nombreUsuario)
                .filter(usuario -> usuario.getRol() != null)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getRol().getActivo()))
                .map(usuario -> (UserDetails) usuario)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontro un usuario con el nombreUsuario: " + nombreUsuario));
    }
}
