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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aviso consolidado al paseador tras liberación nocturna ({@code POST /internal/pagos/liberacion-fondos-consolidada-paseador}).
 */
@Component
public class NotificationLiberacionConsolidadaClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationLiberacionConsolidadaClient.class);

    static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI = "/internal/pagos/liberacion-fondos-consolidada-paseador";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificationLiberacionConsolidadaClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.notification.enabled:false}") boolean enabled,
            @Value("${patiperro.pagos.integracion.notification.base-url:}") String baseUrl,
            @Value("${patiperro.pagos.integracion.notification.interno.secret:}") String internoSecret,
            @Value("${patiperro.pagos.integracion.notification.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.pagos.integracion.notification.read-timeout-ms:15000}") long readTimeoutMs) {
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

    /**
     * Best-effort: no lanza por fallos HTTP/red.
     *
     * @return {@code true} si notification respondió 2xx
     */
    public boolean enviar(Long idUsuarioPaseador, String emailDestino, BigDecimal montoTotalNeto, int cantidadReservas) {
        if (idUsuarioPaseador == null || !StringUtils.hasText(emailDestino) || montoTotalNeto == null || cantidadReservas <= 0) {
            return false;
        }
        if (!isConfigured()) {
            log.debug("Liberación consolidada: notification no configurado; omitido (usuario={})", idUsuarioPaseador);
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idUsuarioPaseador", idUsuarioPaseador);
        body.put("emailDestino", emailDestino.trim());
        body.put("montoTotalNeto", montoTotalNeto.toPlainString());
        body.put("cantidadReservasLiberadas", cantidadReservas);
        try {
            var entity = restClient
                    .post()
                    .uri(URI)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return entity.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            log.warn(
                    "Liberación consolidada: notification HTTP {} (usuario={})",
                    e.getStatusCode().value(),
                    idUsuarioPaseador);
            return false;
        } catch (RestClientException e) {
            log.warn("Liberación consolidada: error de red (usuario={})", idUsuarioPaseador, e);
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
        factory.setReadTimeout(Duration.ofMillis(clamp(readTimeoutMs, 1_000L, 120_000L)));
        return factory;
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
