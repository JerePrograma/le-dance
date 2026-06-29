package ledance.infra.seguridad;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import ledance.entidades.Usuario;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtProperties properties;
    private final Clock clock;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public TokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .build();
    }

    public String generarAccessToken(Usuario usuario) {
        return generarToken(usuario, properties.accessTokenHours(), TokenType.ACCESS);
    }

    public String generarRefreshToken(Usuario usuario) {
        return generarToken(usuario, properties.refreshTokenHours(), TokenType.REFRESH);
    }

    private String generarToken(Usuario usuario, long horas, TokenType tipo) {
        if (usuario.getId() == null || usuario.getNombreUsuario() == null || usuario.getRol() == null) {
            throw new IllegalArgumentException("No se puede generar un token para un usuario incompleto");
        }
        try {
            Instant issuedAt = clock.instant();
            return JWT.create()
                    .withIssuer(properties.issuer())
                    .withSubject(usuario.getNombreUsuario())
                    .withClaim("id", usuario.getId())
                    .withClaim("type", tipo.name())
                    .withClaim("rol", usuario.getRol().getDescripcion())
                    .withJWTId(UUID.randomUUID().toString())
                    .withIssuedAt(Date.from(issuedAt))
                    .withExpiresAt(Date.from(generarFechaExpiracion(issuedAt, horas)))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el token", e);
        }
    }

    public VerifiedToken verify(String token, TokenType expectedType) {
        VerifiedToken verifiedToken = verify(token);
        if (verifiedToken.tokenType() != expectedType) {
            throw new InvalidTokenException();
        }
        return verifiedToken;
    }

    public VerifiedToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException();
        }
        try {
            DecodedJWT decoded = verifier.verify(token);
            String subject = decoded.getSubject();
            Long userId = decoded.getClaim("id").asLong();
            String role = decoded.getClaim("rol").asString();
            String rawType = decoded.getClaim("type").asString();
            Date issuedAt = decoded.getIssuedAt();
            Date expiresAt = decoded.getExpiresAt();
            if (subject == null || subject.isBlank()
                    || userId == null
                    || role == null || role.isBlank()
                    || rawType == null
                    || issuedAt == null
                    || expiresAt == null) {
                throw new InvalidTokenException();
            }
            return new VerifiedToken(
                    subject,
                    userId,
                    role,
                    TokenType.valueOf(rawType),
                    issuedAt.toInstant(),
                    expiresAt.toInstant()
            );
        } catch (InvalidTokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidTokenException(e);
        }
    }

    private Instant generarFechaExpiracion(Instant issuedAt, long horas) {
        return issuedAt.plus(horas, ChronoUnit.HOURS);
    }
}
