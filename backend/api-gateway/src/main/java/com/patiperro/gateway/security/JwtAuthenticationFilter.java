package com.patiperro.gateway.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long paseadorId = jwtService.extractPaseadorId(token);
        Long tutorId = jwtService.extractTutorId(token);

        UsernamePasswordAuthenticationToken authentication;
        if (paseadorId != null) {
            authentication = new UsernamePasswordAuthenticationToken(
                    paseadorId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_PASEADOR")));
        } else if (tutorId != null) {
            authentication = new UsernamePasswordAuthenticationToken(
                    tutorId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        } else {
            String subject = jwtService.extractSubject(token);
            authentication = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    Collections.emptyList());
        }
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Mismo criterio que pagos-service: Bearer del cliente antes que cookie, para no mezclar roles
        // si quedó access_token de otra sesión y el SPA envía el JWT activo en Authorization.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
