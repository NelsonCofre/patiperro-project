package com.patiperro.notification_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT para rutas que exigen autenticación (p. ej. {@code /api/notificaciones/push/**}).
 * Mismo criterio de extracción y orden de claims que {@code pagos-service} / api-gateway.
 * Principal = {@code idUsuario} ({@link Integer}, claim tutorId o paseadorId).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                Long paseadorId = jwtService.extractPaseadorId(token);
                if (paseadorId != null) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    toPrincipalId(paseadorId),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PASEADOR"))));
                } else {
                    Long tutorId = jwtService.extractTutorId(token);
                    if (tutorId != null) {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        toPrincipalId(tutorId),
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_TUTOR"))));
                    }
                }
            } catch (Exception ignored) {
                // Token inválido: no autenticar; SecurityConfig devolverá 401/403 si la ruta lo exige.
            }
        }

        filterChain.doFilter(request, response);
    }

    private static Integer toPrincipalId(Long id) {
        return id != null ? id.intValue() : null;
    }

    private static String extractToken(HttpServletRequest request) {
        // Preferir Bearer explícito antes que cookie (mismo orden que pagos-service).
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        String forwarded = request.getHeader("X-Patiperro-Authorization");
        if (forwarded != null && forwarded.startsWith("Bearer ")) {
            String token = forwarded.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        return extractAccessTokenFromCookieHeader(request.getHeader("X-Patiperro-Forwarded-Cookie"));
    }

    /**
     * Replica del header Cookie cuando el gateway solo reenvía {@code X-Patiperro-Forwarded-Cookie}.
     */
    private static String extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0].trim())) {
                String v = kv[1].trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return null;
    }
}
