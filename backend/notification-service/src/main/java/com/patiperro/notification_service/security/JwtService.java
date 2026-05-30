package com.patiperro.notification_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validación JWT compartida con gateway, tutores, paseadores y pagos ({@code jwt.secret}).
 */
@Service
public class JwtService {

    /** Mismo claim que emite tutores-service. */
    public static final String CLAIM_TUTOR_ID = "tutorId";

    /** Mismo claim que emite paseadores-service. */
    public static final String CLAIM_PASEADOR_ID = "paseadorId";

    /** Claim legacy en algunos JWT de tutor; solo si {@link #CLAIM_TUTOR_ID} no está presente. */
    private static final String CLAIM_USUARIO_ID_LEGACY = "usuarioId";

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Id tutor del token. Incluye fallback {@code usuarioId} para tokens legacy (pagos no lo usa;
     * notification lo conserva por compatibilidad con tutores antiguos).
     */
    public Long extractTutorId(String token) {
        Claims c = parseClaims(token);
        Object v = c.get(CLAIM_TUTOR_ID);
        if (v == null) {
            v = c.get(CLAIM_USUARIO_ID_LEGACY);
        }
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }

    /** {@code null} si el token no incluye el claim (p. ej. JWT de tutor). */
    public Long extractPaseadorId(String token) {
        Claims c = parseClaims(token);
        Object v = c.get(CLAIM_PASEADOR_ID);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }
}
