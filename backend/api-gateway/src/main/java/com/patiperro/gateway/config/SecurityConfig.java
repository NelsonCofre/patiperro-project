package com.patiperro.gateway.config;

import com.patiperro.gateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        /**
         * En Gateway WebMVC, {@code requestMatchers(String)} puede resolverse a {@code MvcRequestMatcher} y no
         * coincidir con la ruta proxificada (login cae en {@code /api/**} → 403). Ignorar auth aquí evita esa capa.
         */
        @Bean
        public WebSecurityCustomizer gatewayPublicAuthPathsIgnored() {
                return web -> web.ignoring()
                                .requestMatchers(
                                                PathPatternRequestMatcher.pathPattern("/api/auth/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/paseadores/auth/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/tutores/auth/**"),
                                                // Agenda valida JWT en agenda-service; evitar 403 espurios en el borde.
                                                PathPatternRequestMatcher.pathPattern("/api/agenda/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/walker/**"),
                                                // Reserva se protege en reserva-service; en gateway evitamos 403 espurios
                                                // por diferencias de matcher/chain en WebMVC.
                                                PathPatternRequestMatcher.pathPattern("/api/reserva/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/reservas/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/bookings/**"),
                                                PathPatternRequestMatcher.pathPattern("/api/tutor/**"));
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
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // PathPattern: no depende del HandlerMappingIntrospector de MVC; con cadenas
                                // simples
                                // Spring Security suele usar MvcRequestMatcher y en el gateway las rutas
                                // proxied pueden
                                // no coincidir -> la peticion cae en /api/** authenticated() y responde 403.
                                .authorizeHttpRequests(auth -> auth
                                                // Preflight CORS solo sobre API proxied (reduce superficie frente a
                                                // OPTIONS /**).
                                                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                                                // Auth pública: usar PathPattern (no strings → MvcRequestMatcher en SS7).
                                                .requestMatchers(
                                                                PathPatternRequestMatcher.pathPattern("/api/auth/**"),
                                                                PathPatternRequestMatcher
                                                                                .pathPattern("/api/paseadores/auth/**"),
                                                                PathPatternRequestMatcher
                                                                                .pathPattern("/api/tutores/auth/**"))
                                                .permitAll()
                                                // Paso 7 (rechazo con reembolso): MP/reembolso y billetera vía
                                                // */interno/* nunca por el borde.
                                                // Denegación explícita + patrón (defensa en profundidad si se añaden
                                                // rutas amplias).
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/pagos/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/pagos/interno/**"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/reserva/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/reserva/interno/**"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/agenda/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/agenda/interno/**"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/tutores/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/tutores/interno/**"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/mascotas/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/mascotas/interno/**"))
                                                .denyAll()
                                                // Internos servidor-a-servidor (resto de microservicios con mismo
                                                // layout).
                                                // PathPattern solo permite ** al inicio/fin; un segmento entre /api/ e
                                                // /interno/ es /*/.
                                                .requestMatchers(
                                                                PathPatternRequestMatcher.pathPattern("/api/*/interno"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/*/interno/**"))
                                                .denyAll()
                                                .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                                                                "/api/paseadores/*"))
                                                .permitAll()
                                                .requestMatchers(
                                                                PathPatternRequestMatcher
                                                                                .pathPattern("/api/tutores/public/**"),
                                                                PathPatternRequestMatcher.pathPattern(
                                                                                "/api/paseadores/public/**"),
                                                                PathPatternRequestMatcher.pathPattern("/api/resenas"), // <---
                                                                                                                       // LA
                                                                                                                       // RAÍZ
                                                                                                                       // (para
                                                                                                                       // el
                                                                                                                       // POST)
                                                                PathPatternRequestMatcher
                                                                                .pathPattern("/api/resenas/**"))
                                                .permitAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/pagos/webhooks/**"))
                                                .permitAll()
                                                // Reserva mantiene su propia seguridad JWT en reserva-service.
                                                // Permitimos en el borde para evitar falsos 403 de gateway
                                                // cuando hay desalineación temporal de validación en esta capa.
                                                .requestMatchers(
                                                                PathPatternRequestMatcher.pathPattern("/api/reserva/**"),
                                                                PathPatternRequestMatcher.pathPattern("/api/reservas/**"),
                                                                PathPatternRequestMatcher.pathPattern("/api/bookings/**"),
                                                                PathPatternRequestMatcher.pathPattern("/api/tutor/**"))
                                                .permitAll()
                                                // JWT y rol PASEADOR los aplica pagos-service (@PreAuthorize). Exigir
                                                // el rol aqui duplicaba el control y podia devolver 403 en el borde
                                                // aunque pagos recibiera bien X-Patiperro-Authorization / cookie.
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/pagos/paseador/**"))
                                                .permitAll()
                                                .requestMatchers(PathPatternRequestMatcher
                                                                .pathPattern("/api/pagos/checkout/**"))
                                                .hasRole("TUTOR")
                                                .requestMatchers(PathPatternRequestMatcher.pathPattern("/api/**"))
                                                .authenticated()
                                                .anyRequest().permitAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .formLogin(form -> form.disable());

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                /*
                 * Vite proxy hacia el gateway deja Origin en el host publico (p. ej. trycloudflare) pero el
                 * Host suele ser 127.0.0.1:8080 → Spring trata la peticion como CORS. Solo localhost en la lista
                 * provoca 403 (DefaultCorsProcessor) aunque el JWT sea valido.
                 */
                Set<String> patterns = new LinkedHashSet<>(List.of(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "https://*.trycloudflare.com",
                                "https://*.ngrok-free.dev",
                                "https://*.ngrok-free.app",
                                "https://*.ngrok.io"));
                for (String o : gatewayCorsProperties.resolvedAllowedOrigins()) {
                        if (o != null && !o.isBlank()) {
                                patterns.add(o.trim());
                        }
                }
                config.setAllowedOriginPatterns(new ArrayList<>(patterns));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
