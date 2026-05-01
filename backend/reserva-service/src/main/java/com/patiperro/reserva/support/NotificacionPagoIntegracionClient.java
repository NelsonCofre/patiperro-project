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
 * Cliente hacia {@code notification-service} ({@code POST /internal/pagos/confirmado}).
 * Deshabilitado por defecto: sin base-url o sin secreto no hace red.
 */
@Component
public class NotificacionPagoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPagoIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /** Correlación servidor-a-servidor para logs en notification-service. */
    static final String HEADER_CORRELATION = "X-Patiperro-Pago-Confirmado-Correlation";

    private static final String URI_CONFIRMADO = "/internal/pagos/confirmado";

    private static final int BODY_DEBUG_MAX_CHARS = 512;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionPagoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.notificacion-pago.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.notificacion-pago.base-url:}") String baseUrl,
            @Value("${patiperro.reserva.integracion.notificacion-pago.interno.secret:}") String internoSecret,
            @Value("${patiperro.reserva.integracion.notificacion-pago.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.reserva.integracion.notificacion-pago.read-timeout-ms:30000}") long readTimeoutMs) {
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
     * Notifica pago confirmado al notification-service (correo al paseador si hay {@code emailDestino}).
     *
     * @return {@code true} si hubo respuesta HTTP 2xx; {@code false} si no se llamó, hubo error de red o status no exitoso
     */
    public boolean notificarPagoConfirmado(Integer idReserva, Integer idPaseador, String emailDestino) {
        if (idReserva == null) {
            return false;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.debug("Notificación pago: integración habilitada pero sin base-url; omitido (reserva={})", idReserva);
            } else if (enabled && restClient != null && !StringUtils.hasText(internoSecret)) {
                log.debug("Notificación pago: falta patiperro.reserva.integracion.notificacion-pago.interno.secret; omitido");
            }
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        if (idPaseador != null) {
            body.put("idPaseador", idPaseador);
        }
        if (StringUtils.hasText(emailDestino)) {
            body.put("emailDestino", emailDestino.trim());
        }
        try {
            var req = restClient.post()
                    .uri(URI_CONFIRMADO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            String correlation = correlationValue(idReserva, idPaseador);
            if (correlation != null) {
                req = req.header(HEADER_CORRELATION, correlation);
            }
            var entity = req.retrieve().toBodilessEntity();
            return entity.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            log.warn("Notificación pago: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode());
            logResponseBodyDebug(e);
            return false;
        } catch (RestClientException e) {
            log.warn("Notificación pago: llamada no completada para reserva {}", idReserva, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("Notificación pago: error inesperado para reserva {}", idReserva, e);
            return false;
        }
    }

    private void logResponseBodyDebug(RestClientResponseException e) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            String raw = e.getResponseBodyAsString();
            log.debug("Notificación pago: cuerpo respuesta (truncado): {}", truncateForLog(raw));
        } catch (RuntimeException ignored) {
            log.debug("Notificación pago: cuerpo respuesta no disponible para log");
        }
    }

    private static String correlationValue(Integer idReserva, Integer idPaseador) {
        if (idReserva == null) {
            return null;
        }
        String pid = idPaseador != null ? String.valueOf(idPaseador) : "na";
        return "reserva-" + idReserva + "-paseador-" + pid;
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
