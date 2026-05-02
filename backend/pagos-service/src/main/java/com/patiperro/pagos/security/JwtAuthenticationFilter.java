package com.patiperro.pagos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
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
}
