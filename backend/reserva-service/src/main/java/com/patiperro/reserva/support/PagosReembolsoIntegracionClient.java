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
 * Solicita reembolso total en pagos-service (Mercado Pago).
 */
@Component
public class PagosReembolsoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(PagosReembolsoIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /** Correlación servidor-a-servidor (logs en pagos-service); no sustituye tracing distribuido. */
    static final String HEADER_CORRELATION = "X-Patiperro-Reembolso-Correlation";

    /** Cabecera estándar para deduplicar POST de reembolso en Mercado Pago (propagada hasta pagos-service). */
    static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String URI_REEMBOLSO = "/api/pagos/interno/mercadopago/reembolso";

    private static final int BODY_DEBUG_MAX_CHARS = 512;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PagosReembolsoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.base-url:http://localhost:8087}") String baseUrl,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.interno.secret:}") String internoSecret,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.read-timeout-ms:30000}") long readTimeoutMs) {
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
     * @return código HTTP de pagos-service (p. ej. 204 éxito, 502 fallo MP), o {@code 0} si no hubo llamada HTTP
     *         (integración deshabilitada / sin configuración)
     */
    public int solicitarReembolsoTotal(Integer idReserva, String mpPaymentId) {
        if (!StringUtils.hasText(mpPaymentId)) {
            return 400;
        }
        if (!isEnabled()) {
            log.debug("Integración reembolso pagos deshabilitada o sin config; sin llamada HTTP (reserva={})", idReserva);
            return 0;
        }
        String trimmedPaymentId = mpPaymentId.trim();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mpPaymentId", trimmedPaymentId);
            if (idReserva != null) {
                body.put("idReserva", idReserva);
            }
            String idempotencyKey = idempotencyKeyReembolso(idReserva, trimmedPaymentId);
            var req = restClient.post()
                    .uri(URI_REEMBOLSO)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            String correlation = correlationValue(idReserva, trimmedPaymentId);
            if (correlation != null) {
                req = req.header(HEADER_CORRELATION, correlation);
            }
            var entity = req.retrieve().toBodilessEntity();
            return entity.getStatusCode().value();
        } catch (RestClientResponseException e) {
            log.warn("Reembolso pagos: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode());
            logResponseBodyDebug(e);
            return e.getStatusCode().value();
        } catch (RestClientException e) {
            log.warn("Reembolso pagos: llamada no completada (reserva={})", idReserva, e);
            return 502;
        } catch (RuntimeException e) {
            log.warn("Reembolso pagos: error inesperado (reserva={})", idReserva, e);
            return 500;
        }
    }

    private void logResponseBodyDebug(RestClientResponseException e) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            String raw = e.getResponseBodyAsString();
            log.debug("Reembolso pagos: cuerpo respuesta (truncado): {}", truncateForLog(raw));
        } catch (RuntimeException ignored) {
            log.debug("Reembolso pagos: cuerpo respuesta no disponible para log");
        }
    }

    /**
     * Debe coincidir con la derivación por defecto en {@code MercadoPagoReembolsoService} (pagos-service)
     * para que los reintentos HTTP reutilicen la misma clave hacia Mercado Pago.
     */
    private static String idempotencyKeyReembolso(Integer idReserva, String mpPaymentId) {
        String rid = idReserva != null ? String.valueOf(idReserva) : "na";
        String pid = StringUtils.hasText(mpPaymentId) ? mpPaymentId.trim().replaceAll("[\\r\\n]", "") : "na";
        String raw = "patiperro-reembolso-reserva-" + rid + "-mp-" + pid;
        if (raw.length() > 255) {
            return raw.substring(0, 255);
        }
        return raw;
    }

    private static String correlationValue(Integer idReserva, String mpPaymentId) {
        if (idReserva == null && !StringUtils.hasText(mpPaymentId)) {
            return null;
        }
        String rid = idReserva != null ? String.valueOf(idReserva) : "na";
        String pid = StringUtils.hasText(mpPaymentId) ? mpPaymentId : "na";
        return "reserva-" + rid + "-mp-" + pid;
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
