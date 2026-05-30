package com.patiperro.chat.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * Participantes de una reserva vía {@code GET /api/reserva/interno/{id}/comprobante}
 * (mismo patrón que pagos-service). No propaga excepciones al llamador.
 */
@Component
public class ReservaChatIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaChatIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_COMPROBANTE = "/api/reserva/interno/{idReserva}/comprobante";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public ReservaChatIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.chat.integracion.reserva.enabled:true}") boolean enabled,
            @Value("${patiperro.chat.integracion.reserva.base-url:http://localhost:8090}") String baseUrl,
            @Value("${patiperro.chat.integracion.reserva.interno.secret:}") String internoSecret,
            @Value("${patiperro.chat.integracion.reserva.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.chat.integracion.reserva.read-timeout-ms:10000}") long readTimeoutMs) {
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

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * {@code null} si la integración está deshabilitada, la reserva no existe o falla la llamada HTTP.
     */
    public ReservaParticipantesDto obtenerParticipantes(Integer idReserva) {
        if (idReserva == null) {
            return null;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.debug("Participantes reserva: integración habilitada pero sin base-url; omitido (reserva={})",
                        idReserva);
            } else if (enabled && !StringUtils.hasText(internoSecret)) {
                log.debug("Participantes reserva: integración habilitada pero sin secreto interno; omitido (reserva={})",
                        idReserva);
            }
            return null;
        }
        try {
            return restClient
                    .get()
                    .uri(URI_COMPROBANTE, idReserva)
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(ReservaParticipantesDto.class);
        } catch (RestClientResponseException e) {
            log.warn("Participantes reserva: HTTP {} (reserva={})", e.getStatusCode(), idReserva);
            return null;
        } catch (RestClientException e) {
            log.warn("Participantes reserva: llamada fallida (reserva={})", idReserva, e);
            return null;
        } catch (RuntimeException e) {
            log.warn("Participantes reserva: error inesperado (reserva={})", idReserva, e);
            return null;
        }
    }

    private static String normalizeBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
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
        factory.setConnectTimeout(Duration.ofMillis(clamp(connectTimeoutMs, 1_000L, 120_000L)));
        factory.setReadTimeout(Duration.ofMillis(clamp(readTimeoutMs, 1_000L, 120_000L)));
        return factory;
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
