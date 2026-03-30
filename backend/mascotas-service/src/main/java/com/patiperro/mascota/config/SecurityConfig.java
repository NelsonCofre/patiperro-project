package com.patiperro.mascota.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (necesario para APIs REST y H2)
                .csrf(csrf -> csrf.disable())
                
                // IMPORTANTE: Permitir que la consola H2 se vea en marcos (frames)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                
                // Configurar manejo de sesiones como Stateless (sin estado)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                .authorizeHttpRequests(auth -> auth
                        // REGLAS PÚBLICAS:
                        .requestMatchers("/api/mascotas/health").permitAll()
                        .requestMatchers("/api/razas/**").permitAll()
                        .requestMatchers("/api/mascotas/auth/**").permitAll()
                        
                        // REGLA PARA H2: Permitir acceso a la consola de la base de datos
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // REGLAS PRIVADAS: Todo lo demás requiere token JWT (como registrar mascota)
                        .anyRequest().authenticated())
                
                // Agregar nuestro filtro de seguridad antes del filtro de usuario/password
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}