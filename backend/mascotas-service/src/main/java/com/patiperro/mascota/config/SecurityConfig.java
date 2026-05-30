package com.patiperro.mascota.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT stateless para tutores (mascotas del dueño autenticado).
 * Fotos de perfil: {@code GET /api/mascotas/public/**} sin token (como tutor/paseador {@code public/**});
 * subida {@code PATCH|POST /api/mascotas/{id}/foto-perfil} requiere JWT.
 * {@code GET /api/mascotas/interno/**} confía en {@code X-Patiperro-Interno-Secret} en el controller;
 * el api-gateway deniega ese prefijo en el borde HTTP público.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Salud y catálogos (comportamiento histórico del servicio).
                        .requestMatchers("/api/mascotas/health").permitAll()
                        .requestMatchers("/api/mascotas/razas/**").permitAll()
                        .requestMatchers("/api/mascotas/especies/**").permitAll()
                        .requestMatchers("/api/mascotas/tamanos/**").permitAll()
                        // Integración S2S: JWT no aplica; secreto en MascotaInternoController (gateway niega /interno/**).
                        .requestMatchers(HttpMethod.GET, "/api/mascotas/interno/**").permitAll()
                        // Servir imágenes (solo GET), como /api/tutores/public/** y /api/paseadores/public/**.
                        .requestMatchers(HttpMethod.GET, "/api/mascotas/public/**").permitAll()
                        // CRUD mascotas, multipart foto-perfil (PATCH/POST), galería: JWT tutor obligatorio.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
