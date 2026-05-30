package com.patiperro.notification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean {@link ObjectMapper} (Jackson 2) para componentes que serializan JSON fuera del stack
 * {@code tools.jackson} de Spring Boot 4 (p. ej. {@link com.patiperro.notification_service.service.WebPushEnvioService}).
 * Mismo patrón que {@code pagos-service}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
