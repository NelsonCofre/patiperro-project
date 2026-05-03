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
    private final GatewayCorsProperties gatewayCorsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GatewayCorsProperties gatewayCorsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.gatewayCorsProperties = gatewayCorsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Seguridad estricta:
        // - login/register tutores y paseadores publicos.
        // - subida de foto perfil paseador ANTES de tener cuenta (flujo registro en el
        // front).
        // - recursos bajo /api/paseadores/public/ (fotos servidas, catalogo tamanos)
        // sin JWT.
        // - resto de /api/** requiere JWT valido.
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // PathPattern: no depende del HandlerMappingIntrospector de MVC; con cadenas
                // simples
                // Spring Security suele usar MvcRequestMatcher y en el gateway las rutas
                // proxied pueden
                // no coincidir -> la peticion cae en /api/** authenticated() y responde 403.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathPatternRequestMatcher.pathPattern("/api/*/interno/**")).denyAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/paseadores/*").permitAll()
                        .requestMatchers(
                                PathPatternRequestMatcher.pathPattern("/api/auth/tutores/register"),
                                PathPatternRequestMatcher.pathPattern("/api/auth/tutores/login"),
                                PathPatternRequestMatcher.pathPattern("/api/auth/tutores/logout"),
                                PathPatternRequestMatcher.pathPattern("/api/tutores/auth/**"),
                                PathPatternRequestMatcher.pathPattern("/api/tutores/public/**"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/register"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/login"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/logout"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/upload-foto-perfil"),
                                PathPatternRequestMatcher.pathPattern("/api/paseadores/public/**"),
                                PathPatternRequestMatcher.pathPattern("/api/resenas"), // <--- LA RAÍZ (para el POST)
                                PathPatternRequestMatcher.pathPattern("/api/resenas/**"))
                        .permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern("/api/pagos/webhooks/**")).permitAll()
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
        config.setAllowedOrigins(List.copyOf(gatewayCorsProperties.getAllowedOrigins()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
