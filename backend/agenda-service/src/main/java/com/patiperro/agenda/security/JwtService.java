package com.patiperro.agenda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Valida JWT emitidos por tutores-service o paseadores-service (misma clave HS256 que el gateway).
 */
@Service
public class JwtService {

    public static final String CLAIM_TUTOR_ID = "tutorId";
    public static final String CLAIM_PASEADOR_ID = "paseadorId";

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractTutorId(String token) {
        return claimAsLong(parseClaims(token).get(CLAIM_TUTOR_ID));
    }

    public Long extractPaseadorId(String token) {
        return claimAsLong(parseClaims(token).get(CLAIM_PASEADOR_ID));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Long claimAsLong(Object raw) {
        if (raw instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
