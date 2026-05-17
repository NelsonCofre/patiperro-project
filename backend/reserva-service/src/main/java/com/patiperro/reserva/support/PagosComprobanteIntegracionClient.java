package com.patiperro.reserva.support;

import com.patiperro.reserva.config.properties.PagosComprobanteIntegracionProperties;
import com.patiperro.reserva.config.properties.PagosReembolsoIntegracionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

/**
 * Invoca {@code POST /api/pagos/interno/comprobante/generar} en pagos-service tras reserva PAGADA.
 */
@Component
public class PagosComprobanteIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(PagosComprobanteIntegracionClient.class);

    public static final String HEADER_INTERNO = PagosReembolsoIntegracionClient.HEADER_INTERNO;

    /** Misma convención que notification-service / pagos-service para encadenar logs. */
    public static final String HEADER_CORRELATION = "X-Patiperro-Comprobante-Correlation";

    private static final String URI_GENERAR = "/api/pagos/interno/comprobante/generar";

    private static final int BODY_DEBUG_MAX_CHARS = 512;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PagosComprobanteIntegracionClient(
            RestClient.Builder restClientBuilder,
            PagosComprobanteIntegracionProperties comprobanteProps,
            PagosReembolsoIntegracionProperties reembolsoProps) {
        this.enabled = comprobanteProps.isEnabled();
        String base = normalizeBaseUrl(
                resolveNonBlank(comprobanteProps.getBaseUrl(), reembolsoProps.getBaseUrl()));
        long connectMs =
                comprobanteProps.getConnectTimeoutMs() > 0
                        ? comprobanteProps.getConnectTimeoutMs()
                        : Math.max(0L, reembolsoProps.getConnectTimeoutMs());
        long readMs = resolveReadTimeoutMs(comprobanteProps, reembolsoProps);
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(connectMs, readMs))
                        .baseUrl(base)
                        .build();
        String secret =
                resolveNonBlank(comprobanteProps.getInterno().getSecret(), reembolsoProps.getInterno().getSecret());
        this.internoSecret = secret != null ? secret.trim() : "";
    }

    private static long resolveReadTimeoutMs(
            PagosComprobanteIntegracionProperties comprobanteProps,
            PagosReembolsoIntegracionProperties reembolsoProps) {
        if (comprobanteProps.getReadTimeoutMs() > 0) {
            return Math.min(Math.max(comprobanteProps.getReadTimeoutMs(), 1_000L), 600_000L);
        }
        long inherited = Math.max(0L, reembolsoProps.getReadTimeoutMs());
        if (inherited <= 0) {
            inherited = 30_000L;
        }
        return Math.min(Math.max(inherited, 1_000L), 45_000L);
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * @return {@code true} solo si pagos respondió 204 No Content.
     */
    public boolean generarYEnviarResumen(Integer idReserva, boolean reenviarCorreo) {
        if (idReserva == null) {
            return false;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.debug("Comprobante pagos: habilitado pero sin base-url; omitido (reserva={})", idReserva);
            } else if (enabled && restClient != null && !StringUtils.hasText(internoSecret)) {
                log.debug("Comprobante pagos: falta secreto interno; omitido");
            }
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("reenviarCorreo", reenviarCorreo);
        try {
            var entity = restClient
                    .post()
                    .uri(URI_GENERAR)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            boolean ok = entity.getStatusCode().value() == 204;
            if (!ok) {
                log.warn(
                        "Comprobante pagos: respuesta inesperada {} (reserva={})",
                        entity.getStatusCode().value(),
                        idReserva);
            }
            return ok;
        } catch (RestClientResponseException e) {
            log.warn(
                    "Comprobante pagos: HTTP {} (reserva={})",
                    e.getStatusCode().value(),
                    idReserva);
            logResponseBodyDebug(e);
            return false;
        } catch (RestClientException e) {
            log.warn("Comprobante pagos: llamada no completada (reserva={})", idReserva, e);
            return false;
        }
    }

    private void logResponseBodyDebug(RestClientResponseException e) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            String raw = e.getResponseBodyAsString();
            log.debug("Comprobante pagos: cuerpo (truncado): {}", truncateForLog(raw));
        } catch (RuntimeException ignored) {
            log.debug("Comprobante pagos: cuerpo no disponible");
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

    private static String resolveNonBlank(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred;
        }
        return fallback != null ? fallback : "";
    }

    private static SimpleClientHttpRequestFactory requestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(connectTimeoutMs, 120_000L))));
        factory.setReadTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(readTimeoutMs, 600_000L))));
        return factory;
    }
}
