package com.patiperro.reserva.config;

import com.patiperro.reserva.controller.InternalPagosController;

import com.patiperro.reserva.security.JwtService;

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.lang.NonNull;

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

            @NonNull HttpServletRequest request,

            @NonNull HttpServletResponse response,

            @NonNull FilterChain filterChain)

            throws ServletException, IOException {

        String path = PATH_HELPER.getPathWithinApplication(request);

        if (path.startsWith("/api/reserva/interno")) {

            // Preflight CORS: no envía cabecera interna; el método real sigue exigiendo secreto.
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

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {

            for (Cookie c : cookies) {

                if ("access_token".equals(c.getName())

                        && c.getValue() != null

                        && !c.getValue().isBlank()) {

                    return c.getValue();

                }

            }

        }

        String header = request.getHeader("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {

            String t = header.substring(7).trim();

            if (!t.isEmpty()) {

                return t;

            }

        }

        return null;

    }

}
