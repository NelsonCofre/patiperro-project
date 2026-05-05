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
                Long tutorId = jwtService.extractTutorId(token);
                if (tutorId != null) {
                    // Sobreescribir usuario anónimo (AnonymousAuthenticationFilter corre antes en la cadena):
                    // si solo se comprobaba getAuthentication()==null, nunca se aplicaba el JWT → 403.
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            tutorId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_TUTOR")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // Token invalido: no autenticar; SecurityConfig devolvera 403 si la ruta lo exige.
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        String tokenFromForwardedCookie = extractAccessTokenFromCookieHeader(
                request.getHeader("X-Patiperro-Forwarded-Cookie"));
        if (tokenFromForwardedCookie != null) {
            return tokenFromForwardedCookie;
        }

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
        return null;
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
