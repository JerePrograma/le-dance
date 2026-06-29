package ledance.infra.seguridad;

import ledance.entidades.Usuario;
import ledance.repositorios.RolRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_ROLE = "ADMINISTRADOR";

    private final AdminBootstrapProperties properties;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            UsuarioRepositorio usuarioRepositorio,
            RolRepositorio rolRepositorio,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepositorio.count() != 0) {
            throw new IllegalStateException(
                    "El bootstrap administrativo sigue habilitado pero ya existen usuarios; deshabilítelo"
            );
        }

        String username = requireUsername(properties.username());
        String password = requirePassword(properties.password());
        var adminRole = rolRepositorio.findByDescripcionIgnoreCase(ADMIN_ROLE)
                .filter(role -> Boolean.TRUE.equals(role.getActivo()))
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR activo"));

        Usuario admin = new Usuario();
        admin.setNombreUsuario(username);
        admin.setContrasena(passwordEncoder.encode(password));
        admin.setRol(adminRole);
        admin.setActivo(true);
        Usuario saved = usuarioRepositorio.save(admin);

        log.warn(
                "Administrador inicial creado con id={}. Deshabilite APP_BOOTSTRAP_ADMIN_ENABLED antes de reiniciar.",
                saved.getId()
        );
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank() || username.length() > 100) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_USERNAME debe tener entre 1 y 100 caracteres");
        }
        return username;
    }

    private String requirePassword(String password) {
        int byteLength = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < 12 || byteLength > 72) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD debe tener entre 12 y 72 bytes UTF-8");
        }
        return password;
    }
}
