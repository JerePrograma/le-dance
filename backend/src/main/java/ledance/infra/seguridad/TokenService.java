package ledance.infra.seguridad;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import ledance.entidades.Usuario;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    private final JwtProperties properties;

    public TokenService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generarAccessToken(Usuario usuario) {
        return generarToken(usuario, properties.accessTokenHours(), "ACCESS");
    }

    public String generarRefreshToken(Usuario usuario) {
        return generarToken(usuario, properties.refreshTokenHours(), "REFRESH");
    }

    private String generarToken(Usuario usuario, long horas, String tipo) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(properties.secret());
            return JWT.create()
                    .withIssuer(properties.issuer())
                    .withSubject(usuario.getNombreUsuario())
                    .withClaim("id", usuario.getId())
                    .withClaim("type", tipo)
                    .withClaim("rol", usuario.getRol().getDescripcion()) // ✅ Añadir rol al token
                    .withExpiresAt(generarFechaExpiracion(horas))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el token", e);
        }
    }

    public String getSubject(String token) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("El token es nulo o vacio");
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(properties.secret());
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token);
            return verifier.getSubject();
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Token invalido o expirado", e);
        }
    }

    public String getRolFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(properties.secret());
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token);
            return verifier.getClaim("rol").asString();
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Error al obtener el rol del token", e);
        }
    }

    private Instant generarFechaExpiracion(long horas) {
        return Instant.now().plus(horas, ChronoUnit.HOURS);
    }

    public String getTokenType(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(properties.secret());
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token);
            return verifier.getClaim("type").asString(); // ✅ Extraer tipo de token
        } catch (JWTVerificationException e) {
            throw new RuntimeException("Error al obtener el tipo de token", e);
        }
    }
}
