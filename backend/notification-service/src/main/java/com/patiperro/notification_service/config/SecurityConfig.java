package com.patiperro.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Vigilancia: Necesario para permitir POST desde Postman
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/notificaciones/**").permitAll() // Libera tus rutas de notificaciones
                .anyRequest().authenticated()
            );
        return http.build();
    }
}