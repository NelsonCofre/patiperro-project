package com.patiperro.tutores.config;

import com.patiperro.tutores.auth.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
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

        String subject = jwtService.extractSubject(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                Collections.emptyList()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        String tokenFromForwardedCookie = extractAccessTokenFromCookieHeader(
                request.getHeader("X-Patiperro-Forwarded-Cookie"));
        if (tokenFromForwardedCookie != null) {
            return tokenFromForwardedCookie;
        }

        // Fallback para pruebas con Postman/CLI usando Bearer header.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String forwardedAuthHeader = request.getHeader("X-Patiperro-Authorization");
        if (forwardedAuthHeader != null && forwardedAuthHeader.startsWith("Bearer ")) {
            return forwardedAuthHeader.substring(7);
        }
        return null;
    }

    private static String extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0].trim())) {
                String value = kv[1].trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }
}
