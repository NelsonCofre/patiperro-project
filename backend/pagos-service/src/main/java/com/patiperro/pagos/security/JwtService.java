package com.patiperro.pagos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

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

    /** Mismo claim que emite tutores-service ({@code tutorId} = id tutor en BD, alineado con {@code id_tutor_usuario} en reserva). */
    public static final String CLAIM_TUTOR_ID = "tutorId";

    /** Igual que {@code com.patiperro.paseador.auth.service.JwtService#CLAIM_PASEADOR_ID} (coincide con {@code id_usuario} en agenda / billetera). */
    public static final String CLAIM_PASEADOR_ID = "paseadorId";

    public Long extractTutorId(String token) {
        Claims c = parseClaims(token);
        Object v = c.get(CLAIM_TUTOR_ID);
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
