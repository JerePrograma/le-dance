package ledance.infra.seguridad;

import com.auth0.jwt.JWT;
import ledance.entidades.Rol;
import ledance.entidades.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    @Test
    void usaEmisorTipoYDuracionConfigurados() {
        TokenService service = new TokenService(new JwtProperties(
                "test-only-secret-with-at-least-32-characters",
                "le-dance-test",
                1,
                24
        ));
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombreUsuario("tester");
        usuario.setRol(new Rol(1L, "ADMIN", true));

        String token = service.generarAccessToken(usuario);
        var decoded = JWT.decode(token);
        long minutes = Duration.between(Instant.now(), decoded.getExpiresAtAsInstant()).toMinutes();

        assertEquals("le-dance-test", decoded.getIssuer());
        assertEquals("ACCESS", decoded.getClaim("type").asString());
        assertEquals("tester", service.getSubject(token));
        assertTrue(minutes >= 59 && minutes <= 60);
    }
}
