package com.patiperro.pagos.support;

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
 * Cliente pagos-service → notification-service para enviar el resumen de pago al tutor.
 * Deshabilitado por defecto: sin base-url o sin secreto no hace red.
 */
@Component
public class NotificacionResumenPagoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionResumenPagoIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_RESUMEN_TUTOR = "/internal/pagos/resumen-pago-tutor";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionResumenPagoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.notification.enabled:false}") boolean enabled,
            @Value("${patiperro.pagos.integracion.notification.base-url:}") String baseUrl,
            @Value("${patiperro.pagos.integracion.notification.interno.secret:}") String internoSecret,
            @Value("${patiperro.pagos.integracion.notification.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.pagos.integracion.notification.read-timeout-ms:15000}") long readTimeoutMs
    ) {
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

    public boolean enviarResumenTutor(Long idReserva, Long idTutorUsuario, String emailDestino, String htmlResumen) {
        if (idReserva == null || !StringUtils.hasText(emailDestino) || !StringUtils.hasText(htmlResumen)) {
            return false;
        }
        if (!isEnabled()) {
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        if (idTutorUsuario != null) {
            body.put("idTutorUsuario", idTutorUsuario);
        }
        body.put("emailDestino", emailDestino.trim());
        body.put("htmlResumen", htmlResumen);
        try {
            var entity = restClient.post()
                    .uri(URI_RESUMEN_TUTOR)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return entity.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            log.warn("Enviar resumen tutor: notification-service respondió HTTP {} (idReserva={})", e.getStatusCode(), idReserva);
            return false;
        } catch (RestClientException e) {
            log.warn("Enviar resumen tutor: notification-service no disponible (idReserva={})", idReserva, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("Enviar resumen tutor: error inesperado (idReserva={})", idReserva, e);
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

