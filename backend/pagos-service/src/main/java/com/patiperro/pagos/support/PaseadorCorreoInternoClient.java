package com.patiperro.pagos.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Obtiene correo del paseador por id de usuario ({@code GET /api/paseadores/interno/{id}/correo}).
 */
@Component
public class PaseadorCorreoInternoClient {

    private static final Logger log = LoggerFactory.getLogger(PaseadorCorreoInternoClient.class);

    static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PaseadorCorreoInternoClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.paseadores.enabled:false}") boolean enabled,
            @Value("${patiperro.pagos.integracion.paseadores.base-url:}") String baseUrl,
            @Value("${patiperro.pagos.integracion.paseadores.interno.secret:}") String internoSecret,
            @Value("${patiperro.pagos.integracion.paseadores.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${patiperro.pagos.integracion.paseadores.read-timeout-ms:10000}") long readTimeoutMs) {
        this.enabled = enabled;
        String base = normalizeBaseUrl(baseUrl);
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                        .baseUrl(base)
                        .build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isConfigured() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /** @ return correo o cadena vacía si no hay datos o falla la llamada */
    public String obtenerCorreo(Long idUsuarioPaseador) {
        if (idUsuarioPaseador == null || !isConfigured()) {
            return "";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient
                    .get()
                    .uri("/api/paseadores/interno/{id}/correo", idUsuarioPaseador)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(Map.class);
            if (body == null || body.get("correo") == null) {
                return "";
            }
            String c = String.valueOf(body.get("correo"));
            return c != null ? c.trim() : "";
        } catch (RestClientResponseException e) {
            log.warn("Correo paseador interno: HTTP {} (usuario={})", e.getStatusCode().value(), idUsuarioPaseador);
            return "";
        } catch (RestClientException e) {
            log.warn("Correo paseador interno: red (usuario={})", idUsuarioPaseador, e);
            return "";
        }
    }

    private static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String b = raw.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static SimpleClientHttpRequestFactory requestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(clamp(connectTimeoutMs, 500L, 60_000L)));
        factory.setReadTimeout(Duration.ofMillis(clamp(readTimeoutMs, 500L, 120_000L)));
        return factory;
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
