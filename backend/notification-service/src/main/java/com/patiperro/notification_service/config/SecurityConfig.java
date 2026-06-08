package com.patiperro.notification_service.config;

import com.patiperro.notification_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Seguridad HTTP del notification-service.
 * <p><strong>Web Push:</strong> {@code GET /push/vapid-public-key} es público; el resto de
 * {@code /api/notificaciones/push/**} exige JWT (suscripciones).</p>
 * <p>El resto de {@code /api/notificaciones/**} sigue abierto aquí (plantillas, Brevo); el gateway
 * proxifica solo ese prefijo público.</p>
 * <p>{@code /internal/**} queda en {@code permitAll} a nivel Spring; cada controller valida
 * {@code X-Patiperro-Interno-Secret}. No exponer el puerto del microservicio a Internet.</p>
 * <p><strong>Orden:</strong> reglas más específicas (push, vapid) antes que
 * {@code /api/notificaciones/**}.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PATH_PUSH = "/api/notificaciones/push/**";
    private static final String PATH_PUSH_VAPID_PUBLIC_KEY = "/api/notificaciones/push/vapid-public-key";
    private static final String PATH_NOTIFICACIONES_API = "/api/notificaciones/**";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**")
                        .permitAll()
                        // Web Push: clave VAPID pública (estándar); POST/DELETE suscripciones → authenticated.
                        .requestMatchers(HttpMethod.GET, PATH_PUSH_VAPID_PUBLIC_KEY).permitAll()
                        .requestMatchers(PATH_PUSH).authenticated()
                        .requestMatchers(PATH_NOTIFICACIONES_API).permitAll()
                        // Servidor-a-servidor: secreto en Internal*Controller (no JWT de usuario).
                        .requestMatchers("/internal/paseo/**").permitAll()
                        .requestMatchers("/internal/pagos/**").permitAll()
                        .requestMatchers("/internal/chat/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        Set<String> patterns = new LinkedHashSet<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.trycloudflare.com",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app",
                "https://*.ngrok.io"));
        config.setAllowedOriginPatterns(new ArrayList<>(patterns));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Cache-Control",
                "X-Patiperro-Authorization",
                "X-Patiperro-Forwarded-Cookie"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
