package ledance.infra.seguridad;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import ledance.entidades.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    // Genera un Access Token (corto plazo), p. ej. 2 horas
    public String generarAccessToken(Usuario usuario) {
        return generarToken(usuario, 2, "ACCESS");
    }

    // Genera un Refresh Token (largo plazo), p. ej. 7 días
    public String generarRefreshToken(Usuario usuario) {
        return generarToken(usuario, 24 * 7, "REFRESH");
    }

    // Método privado para crear un token con un "claim" de tipo (ACCESS o REFRESH)
    private String generarToken(Usuario usuario, int horas, String tipo) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("ledance")
                    .withSubject(usuario.getEmail())
                    .withClaim("id", usuario.getId())
                    .withClaim("type", tipo)
                    .withExpiresAt(generarFechaExpiracion(horas))
                    .sign(algorithm);

        } catch (JWTCreationException e) {
            throw new RuntimeException("Error al generar el token", e);
        }
    }

    public String getSubject(String token) {
        if (token == null) {
            throw new RuntimeException("El token es nulo");
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer("ledance")
                    .build()
                    .verify(token);
            return verifier.getSubject();
        } catch (TokenExpiredException e) {
            throw new RuntimeException("El token ha expirado", e);
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Token invalido o no verificable", e);
        }
    }

    // Opcional: para verificar si es Access o Refresh
    public String getTokenType(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer("ledance")
                    .build()
                    .verify(token);
            return verifier.getClaim("type").asString(); // "ACCESS" o "REFRESH"
        } catch (TokenExpiredException e) {
            throw new RuntimeException("El token ha expirado", e);
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Token invalido o no verificable", e);
        }
    }

    private Instant generarFechaExpiracion(int horas) {
        return LocalDateTime.now().plusHours(horas).toInstant(ZoneOffset.of("-03:00"));
    }
}
