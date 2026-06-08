package com.patiperro.reserva.config;

import com.patiperro.reserva.controller.InternalPagosController;
import com.patiperro.reserva.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final UrlPathHelper PATH_HELPER = UrlPathHelper.defaultInstance;
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String HEADER_DOWNSTREAM_AUTH = "X-Patiperro-Authorization";
    private static final String HEADER_DOWNSTREAM_COOKIE = "X-Patiperro-Forwarded-Cookie";

    private final JwtService jwtService;
    private final String internoSecret;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${patiperro.reserva.interno.secret:}") String internoSecret) {
        this.jwtService = jwtService;
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String path = PATH_HELPER.getPathWithinApplication(request);

        if (path.startsWith("/api/reserva/interno")) {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!StringUtils.hasText(internoSecret)) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            String header = request.getHeader(InternalPagosController.HEADER_INTERNO);
            if (!internoSecret.equals(header)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

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
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                Collections.emptyList());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Bearer antes que cookie: el SPA envía sessionStorage en Authorization; cookie antigua de otro rol puede ganar.
        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token != null) {
            return token;
        }

        token = extractBearerToken(request.getHeader(HEADER_DOWNSTREAM_AUTH));
        if (token != null) {
            return token;
        }

        token = extractAccessTokenFromCookies(request.getCookies());
        if (token != null) {
            return token;
        }

        return extractAccessTokenFromCookieHeader(request.getHeader(HEADER_DOWNSTREAM_COOKIE));
    }

    private String extractAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private String extractBearerToken(String header) {
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    private String extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] fragments = cookieHeader.split(";");
        for (String fragment : fragments) {
            String[] pair = fragment.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String name = pair[0].trim();
            String value = pair[1].trim();
            if (ACCESS_TOKEN_COOKIE_NAME.equals(name) && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
