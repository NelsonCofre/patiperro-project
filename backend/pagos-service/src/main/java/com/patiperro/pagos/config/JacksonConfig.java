package com.patiperro.pagos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fuerza la disponibilidad de {@link ObjectMapper} para componentes que lo inyectan
 * (p. ej. MercadoPagoApiClient) aun si la autoconfiguración JSON no está activa.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}

