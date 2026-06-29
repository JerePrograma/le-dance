package ledance.servicios.usuario;

import ledance.dto.usuario.UsuarioMapper;
import ledance.entidades.Usuario;
import ledance.repositorios.RolRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServicioTest {

    @Test
    void eliminarUsuarioEsBajaLogicaYConservaReferenciasHistoricas() {
        UsuarioRepositorio usuarioRepositorio = mock(UsuarioRepositorio.class);
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setActivo(true);
        when(usuarioRepositorio.findById(8L)).thenReturn(Optional.of(usuario));
        UsuarioServicio service = new UsuarioServicio(
                usuarioRepositorio,
                mock(PasswordEncoder.class),
                mock(RolRepositorio.class),
                mock(UsuarioMapper.class)
        );

        service.eliminarUsuario(8L);

        assertFalse(usuario.getActivo());
        verify(usuarioRepositorio).save(usuario);
        verify(usuarioRepositorio, never()).deleteById(8L);
    }
}
