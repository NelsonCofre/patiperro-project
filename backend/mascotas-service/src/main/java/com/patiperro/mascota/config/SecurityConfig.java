package com.patiperro.mascota.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                        // 1. VIGILANCIA: Rutas públicas (No necesitan Token) //
                        .requestMatchers("/api/mascotas/health").permitAll()
                        .requestMatchers("/api/mascotas/razas/**").permitAll()
                        .requestMatchers("/api/mascotas/especies/**").permitAll()
                        .requestMatchers("/api/mascotas/tamanos/**").permitAll()
                        
                        // 2. VIGILANCIA: Todo lo demás (Registrar, Borrar, Editar) requiere login //
                        .anyRequest().authenticated())
                
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}