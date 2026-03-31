package com.patiperro.tutores.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public static final String CLAIM_TUTOR_ID = "tutorId";

    /**
     * JWT para tutor: subject = correo; claim {@value #CLAIM_TUTOR_ID} = id en BD (tutor.id_tutor).
     * Otros microservicios con la misma clave pueden validar el token y leer el id sin llamar a tutores.
     */
    public String generateToken(String correo, Long idTutor) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_TUTOR_ID, idTutor)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

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
        Claims claims = parseClaims(token);
        Object raw = claims.get(CLAIM_TUTOR_ID);
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
