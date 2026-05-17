package com.patiperro.pagos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                // Mismo criterio que api-gateway: paseador antes que tutor. Si se evaluaba tutor primero,
                // un claim legacy {@code usuarioId} podía clasificar mal un JWT de paseador como TUTOR y
                // bloquear {@code @PreAuthorize("hasRole('PASEADOR')")} en billetera (403 en POST/GET).
                Long paseadorId = jwtService.extractPaseadorId(token);
                if (paseadorId != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            paseadorId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_PASEADOR")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    Long tutorId = jwtService.extractTutorId(token);
                    if (tutorId != null) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                tutorId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_TUTOR")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // Token invalido: no autenticar; SecurityConfig devolvera 403 si la ruta lo exige.
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String extractToken(HttpServletRequest request) {
        // Preferir Bearer explícito antes que cookie: el SPA envía sessionStorage en Authorization;
        // una cookie access_token antigua (p. ej. sesión tutor) podía ganar y forzar 403 en rutas PASEADOR.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        // Gateway WebMVC no reenvía Authorization; duplicamos en este header desde api-gateway.
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
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
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
