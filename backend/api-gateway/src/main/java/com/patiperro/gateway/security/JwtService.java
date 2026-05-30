package com.patiperro.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

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

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Alineado con pagos-service / tutores-service (claim {@code tutorId} o fallback {@code usuarioId}). */
    public Long extractTutorId(String token) {
        try {
            Claims c = parseClaims(token);
            Object v = c.get("tutorId");
            if (v == null) {
                v = c.get("usuarioId");
            }
            if (v == null) {
                return null;
            }
            if (v instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(v.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Alineado con paseadores-service / pagos-service ({@code paseadorId} = id usuario paseador). */
    public Long extractPaseadorId(String token) {
        try {
            Claims c = parseClaims(token);
            Object v = c.get("paseadorId");
            if (v == null) {
                return null;
            }
            if (v instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(v.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
