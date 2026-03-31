package com.patiperro.mascota.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Misma clave que tutores-service. Subject = correo; claim {@value #CLAIM_TUTOR_ID} = id en tutor.id_tutor.
 */
@Service
public class JwtService {

    /** Debe coincidir con {@code JwtService.CLAIM_TUTOR_ID} en tutores-service. */
    public static final String CLAIM_TUTOR_ID = "tutorId";

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
        if (raw == null) {
            return null;
        }
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
