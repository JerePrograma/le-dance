package ledance.infra.seguridad;

import ledance.entidades.Rol;
import ledance.entidades.Usuario;
import ledance.repositorios.RolRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTest {

    private final UsuarioRepositorio usuarioRepositorio = mock(UsuarioRepositorio.class);
    private final RolRepositorio rolRepositorio = mock(RolRepositorio.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void creaUnicoAdministradorSinExponerNiPersistirClavePlana() throws Exception {
        Rol role = new Rol(1L, "ADMINISTRADOR", true);
        when(usuarioRepositorio.count()).thenReturn(0L);
        when(rolRepositorio.findByDescripcionIgnoreCase("ADMINISTRADOR"))
                .thenReturn(Optional.of(role));
        when(passwordEncoder.encode("clave-inicial-segura"))
                .thenReturn("bcrypt-hash");
        when(usuarioRepositorio.save(org.mockito.ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario user = invocation.getArgument(0);
                    user.setId(10L);
                    return user;
                });

        runner("admin-inicial", "clave-inicial-segura")
                .run(new DefaultApplicationArguments());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositorio).save(captor.capture());
        assertEquals("admin-inicial", captor.getValue().getNombreUsuario());
        assertEquals("bcrypt-hash", captor.getValue().getContrasena());
        assertEquals(role, captor.getValue().getRol());
        assertTrue(captor.getValue().getActivo());
    }

    @Test
    void exigeDeshabilitarBootstrapSiYaExisteUnUsuario() {
        when(usuarioRepositorio.count()).thenReturn(1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner("admin", "clave-inicial-segura")
                        .run(new DefaultApplicationArguments())
        );

        assertTrue(exception.getMessage().contains("deshabilítelo"));
    }

    @Test
    void rechazaCredencialesBootstrapVaciasOCortas() {
        when(usuarioRepositorio.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class,
                () -> runner("", "clave-inicial-segura")
                        .run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class,
                () -> runner("admin", "corta")
                        .run(new DefaultApplicationArguments()));
    }

    private AdminBootstrapRunner runner(String username, String password) {
        return new AdminBootstrapRunner(
                new AdminBootstrapProperties(true, username, password),
                usuarioRepositorio,
                rolRepositorio,
                passwordEncoder
        );
    }
}
