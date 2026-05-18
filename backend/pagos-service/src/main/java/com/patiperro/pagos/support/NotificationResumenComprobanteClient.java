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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envío del HTML del resumen al tutor vía {@code notification-service}
 * ({@code POST /internal/pagos/resumen-comprobante-tutor}).
 */
@Component
public class NotificationResumenComprobanteClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationResumenComprobanteClient.class);

    static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /** Correlación servidor-a-servidor para diagnóstico en notification-service. */
    static final String HEADER_CORRELATION = "X-Patiperro-Comprobante-Correlation";

    private static final String URI = "/internal/pagos/resumen-comprobante-tutor";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;
    private final int maxHtmlChars;

    public NotificationResumenComprobanteClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.notification.enabled:false}") boolean enabled,
            @Value("${patiperro.pagos.integracion.notification.base-url:}") String baseUrl,
            @Value("${patiperro.pagos.integracion.notification.interno.secret:}") String internoSecret,
            @Value("${patiperro.pagos.integracion.notification.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.pagos.integracion.notification.read-timeout-ms:30000}") long readTimeoutMs,
            @Value("${patiperro.pagos.integracion.notification.max-html-chars:200000}") int maxHtmlChars) {
        this.enabled = enabled;
        String base = normalizeBaseUrl(baseUrl);
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                        .baseUrl(base)
                        .build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
        this.maxHtmlChars = clampInt(maxHtmlChars, 10_000, 2_000_000);
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * @return {@code true} si notification respondió 2xx
     */
    public boolean enviarResumenHtml(Integer idReserva, String emailDestino, String htmlUtf8) {
        if (idReserva == null || !StringUtils.hasText(emailDestino) || htmlUtf8 == null) {
            return false;
        }
        if (!isEnabled()) {
            log.debug("Resumen comprobante tutor: integración notification deshabilitada o sin config (reserva={})", idReserva);
            return false;
        }
        if (htmlUtf8.length() > maxHtmlChars) {
            log.warn(
                    "Resumen comprobante tutor: HTML demasiado grande; omitido para evitar abuso (reserva={}, len={}, max={})",
                    idReserva,
                    htmlUtf8.length(),
                    maxHtmlChars);
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("emailDestino", emailDestino.trim());
        body.put("cuerpoHtml", htmlUtf8);
        try {
            var entity = restClient
                    .post()
                    .uri(URI)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION, correlationValue(idReserva))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return entity.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            log.warn(
                    "Resumen comprobante tutor: notification HTTP {} (reserva={})",
                    e.getStatusCode().value(),
                    idReserva);
            return false;
        } catch (RestClientException e) {
            log.warn("Resumen comprobante tutor: error de red (reserva={})", idReserva, e);
            return false;
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
        factory.setConnectTimeout(Duration.ofMillis(clamp(connectTimeoutMs, 1_000L, 120_000L)));
        factory.setReadTimeout(Duration.ofMillis(clamp(readTimeoutMs, 1_000L, 600_000L)));
        return factory;
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String correlationValue(Integer idReserva) {
        return "reserva-" + idReserva;
    }
}
