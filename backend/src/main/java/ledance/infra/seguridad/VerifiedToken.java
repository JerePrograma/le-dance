package ledance.infra.seguridad;

import java.time.Instant;

public record VerifiedToken(
        String subject,
        Long userId,
        String role,
        TokenType tokenType,
        Instant issuedAt,
        Instant expiresAt
) {
}
