package com.patiperro.reserva.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Garantiza un {@link ObjectMapper} en el contexto (p. ej. {@code PagosCheckoutIntegracionClient})
 * si el auto-config de Jackson no registra el bean en este arranque.
 */
@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}
