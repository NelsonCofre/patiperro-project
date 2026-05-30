package com.patiperro.pagos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS cuando el navegador llama directo a pagos-service (p. ej. {@code VITE_PAGOS_CHECKOUT_API_BASE}).
 * En dev normal, el front usa el gateway vía {@code /api} y aquí casi no aplica; estos patrones cubren
 * otros puertos locales y túneles si alguien sigue apuntando a {@code :8087}.
 */
@Configuration
public class PagosCorsConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patrones: cualquier puerto local (Vite cambia) y túneles típicos si algo sigue llamando directo a :8087.
        // No mezclar con setAllowedOrigins (Spring exige uno u otro).
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.trycloudflare.com",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app",
                "https://*.ngrok.io"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Patiperro-Authorization",
                "X-Patiperro-Forwarded-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
