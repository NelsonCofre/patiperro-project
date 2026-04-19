package com.patiperro.reserva.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    public static final String CLAIM_TUTOR_ID = "tutorId";

    /** Mismo claim que emite paseadores-service en el JWT del paseador. */
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
        Object raw = parseClaims(token).get(CLAIM_TUTOR_ID);
        if (raw instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    public Long extractPaseadorId(String token) {
        Object raw = parseClaims(token).get(CLAIM_PASEADOR_ID);
        if (raw instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
