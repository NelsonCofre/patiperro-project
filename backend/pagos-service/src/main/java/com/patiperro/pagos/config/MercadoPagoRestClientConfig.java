package com.patiperro.pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} dedicado a Mercado Pago (timeouts y base URL aislados del resto del servicio).
 */
@Configuration
public class MercadoPagoRestClientConfig {

    public static final String MERCADOPAGO_REST_CLIENT_BEAN = "mercadoPagoRestClient";

    @Bean(name = MERCADOPAGO_REST_CLIENT_BEAN)
    public RestClient mercadoPagoRestClient(
            @Value("${patiperro.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
            @Value("${patiperro.mercadopago.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${patiperro.mercadopago.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1000, connectTimeoutMs));
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
        String base = normalizeBaseUrl(baseUrl);
        return RestClient.builder()
                .baseUrl(base)
                .requestFactory(factory)
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String base = baseUrl == null || baseUrl.isBlank() ? "https://api.mercadopago.com" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
