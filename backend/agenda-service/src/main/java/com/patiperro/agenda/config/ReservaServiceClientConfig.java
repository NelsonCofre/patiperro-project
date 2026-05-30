package com.patiperro.agenda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class ReservaServiceClientConfig {

    /** Mismo valor que {@code X-Patiperro-Interno-Secret} en reserva-service (sin dependencia Maven cruzada). */
    private static final String HEADER_RESERVA_INTERNO = "X-Patiperro-Interno-Secret";

    @Bean
    public RestClient reservaRestClient(
            @Value("${patiperro.reserva-service.base-url}") String baseUrl,
            @Value("${patiperro.agenda.reserva-interno.secret:}") String reservaInternoSecret) {
        RestClient.Builder b = RestClient.builder().baseUrl(baseUrl.trim());
        if (StringUtils.hasText(reservaInternoSecret)) {
            b.defaultHeader(HEADER_RESERVA_INTERNO, reservaInternoSecret.trim());
        }
        return b.build();
    }
}