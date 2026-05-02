package com.patiperro.reserva.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Perfil {@code test}: algunos arranques slice/full no exponen {@link ObjectMapper}
 * (necesario para {@link com.patiperro.reserva.support.PagosCheckoutIntegracionClient}).
 */
@TestConfiguration
public class JacksonTestSupportConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
