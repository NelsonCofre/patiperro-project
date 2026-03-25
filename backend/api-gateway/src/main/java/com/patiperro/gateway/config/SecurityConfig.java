package com.patiperro.gateway.config;

import com.patiperro.gateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Seguridad del gateway:
        // - /api/auth/tutores/** y /api/paseadores/auth/** publicos.
        // - resto de /api/** requiere Bearer token valido.
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // PathPattern: no depende del HandlerMappingIntrospector de MVC; con cadenas simples
                // Spring Security suele usar MvcRequestMatcher y en el gateway las rutas proxied pueden
                // no coincidir -> la peticion cae en /api/** authenticated() y responde 403.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                PathPatternRequestMatcher.pathPattern("/api/auth/tutores/**"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/**"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/health"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/public/**"),
                                PathPatternRequestMatcher.pathPattern("/api/tutores/public/**"))
                        .permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern("/api/**")).authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Vite puede usar 5173 u otro puerto libre (ej. 5174 si 5173 esta ocupado)
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
