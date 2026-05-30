package com.patiperro.paseador.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT stateless para paseadores. Rutas bajo {@code /api/paseadores/me/**} (perfil, verificación
 * de identidad con cédula) requieren autenticación. {@code /api/paseadores/interno/**} confía en
 * {@code X-Patiperro-Interno-Secret} en el controller; el api-gateway deniega ese prefijo en el
 * borde HTTP público.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/paseadores/auth/**").permitAll()
                        .requestMatchers("/api/paseadores/public/**").permitAll()
                        .requestMatchers("/api/paseadores/interno/**").permitAll()
                        .requestMatchers("/api/paseadores/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/paseadores/*").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
