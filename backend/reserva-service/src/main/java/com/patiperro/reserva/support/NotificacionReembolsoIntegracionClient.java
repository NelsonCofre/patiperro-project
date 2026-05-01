package com.patiperro.reserva.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Notifica al tutor que se procesó la devolución vía notification-service.
 */
@Component
public class NotificacionReembolsoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionReembolsoIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    static final String HEADER_CORRELATION = "X-Patiperro-Reembolso-Tutor-Correlation";

    private static final String URI_REEMBOLSO = "/internal/pagos/reembolso-tutor";

    private static final int BODY_DEBUG_MAX_CHARS = 512;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionReembolsoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.notificacion-reembolso.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.notificacion-reembolso.base-url:http://localhost:8086}") String baseUrl,
            @Value("${patiperro.reserva.integracion.notificacion-reembolso.interno.secret:}") String internoSecret,
            @Value("${patiperro.reserva.integracion.notificacion-reembolso.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.reserva.integracion.notificacion-reembolso.read-timeout-ms:30000}") long readTimeoutMs) {
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
     * @return {@code true} si notification-service respondió 2xx
     */
    public boolean notificarReembolsoTutor(Integer idReserva, String emailDestino) {
        if (idReserva == null) {
            return false;
        }
        if (!isEnabled()) {
            log.debug("Notificación reembolso: integración deshabilitada; omitido (reserva={})", idReserva);
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        if (StringUtils.hasText(emailDestino)) {
            body.put("emailDestino", emailDestino.trim());
        }
        try {
            restClient.post()
                    .uri(URI_REEMBOLSO)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION, "reserva-" + idReserva)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.warn("Notificación reembolso: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode());
            logResponseBodyDebug(e);
            return false;
        } catch (RestClientException e) {
            log.warn("Notificación reembolso: llamada no completada (reserva={})", idReserva, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("Notificación reembolso: error inesperado (reserva={})", idReserva, e);
            return false;
        }
    }

    private void logResponseBodyDebug(RestClientResponseException e) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            String raw = e.getResponseBodyAsString();
            log.debug("Notificación reembolso: cuerpo respuesta (truncado): {}", truncateForLog(raw));
        } catch (RuntimeException ignored) {
            log.debug("Notificación reembolso: cuerpo respuesta no disponible para log");
        }
    }

    private static String truncateForLog(String body) {
        if (body == null) {
            return "";
        }
        String t = body.trim().replaceAll("\\s+", " ");
        if (t.length() <= BODY_DEBUG_MAX_CHARS) {
            return t;
        }
        return t.substring(0, BODY_DEBUG_MAX_CHARS) + "…";
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
        factory.setConnectTimeout(Duration.ofMillis(clampTimeoutMs(connectTimeoutMs, 1_000L, 120_000L)));
        factory.setReadTimeout(Duration.ofMillis(clampTimeoutMs(readTimeoutMs, 1_000L, 600_000L)));
        return factory;
    }

    private static long clampTimeoutMs(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
