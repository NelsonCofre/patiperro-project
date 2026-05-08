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

/**
 * Tras marcar reserva PAGADA: invoca pagos-service para generar/persistir comprobante y (opcional) correo al tutor.
 * Best-effort; ejecutar después de COMMIT desde {@code ReservaPagoService}.
 */
@Component
public class PagosComprobanteIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(PagosComprobanteIntegracionClient.class);

    private static final String HEADER_INTERNO = PagosReembolsoIntegracionClient.HEADER_INTERNO;

    /** Correlación servidor-a-servidor (solo idReserva; sin secretos ni cuerpo). */
    static final String HEADER_CORRELATION = "X-Patiperro-Comprobante-Correlation";

    private static final String URI_GENERAR_ENVIAR = "/api/pagos/interno/comprobante/generar-y-enviar";

    /**
     * Si no defines {@code patiperro.reserva.integracion.pagos-comprobante.read-timeout-ms}
     * y heredas el timeout largo de reembolso, acotamos lectura para no bloquear hilos post-{@code COMMIT} demasiado tiempo.
     */
    private static final long READ_TIMEOUT_MS_FALLBACK_CAP = 45_000L;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PagosComprobanteIntegracionClient(
            RestClient.Builder restClientBuilder,
            PagosComprobanteIntegracionProperties comprobanteProps,
            PagosReembolsoIntegracionProperties reembolsoProps) {
        String base = normalizeBaseUrl(resolveNonBlank(comprobanteProps.getBaseUrl(), reembolsoProps.getBaseUrl()));
        long connectTimeoutMs = resolveLong(comprobanteProps.getConnectTimeoutMs(), reembolsoProps.getConnectTimeoutMs());
        long readTimeoutMs = resolveReadTimeoutMs(comprobanteProps, reembolsoProps);
        String secret = resolveNonBlank(comprobanteProps.getInterno().getSecret(), reembolsoProps.getInterno().getSecret());
        this.internoSecret = secret != null ? secret.trim() : "";
        this.enabled = comprobanteProps.isEnabled() && StringUtils.hasText(base) && StringUtils.hasText(this.internoSecret);
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                        .baseUrl(base)
                        .build();
        logEstadoArranque(comprobanteProps.isEnabled(), base, this.internoSecret);
    }

    private void logEstadoArranque(boolean solicitadoPorPropiedad, String baseUrlEfectiva, String secretoEfectivo) {
        if (!solicitadoPorPropiedad) {
            return;
        }
        if (!isEnabled()) {
            if (!StringUtils.hasText(baseUrlEfectiva)) {
                log.warn(
                        "pagos-comprobante: RESERVA_INTEGRACION_PAGOS_COMPROBANTE_ENABLED=true pero no hay base-url efectiva; "
                                + "definir RESERVA_INTEGRACION_PAGOS_COMPROBANTE_BASE_URL o patiperro.reserva.integracion.pagos-reembolso.base-url "
                                + "(p. ej. PAGOS_SERVICE_URL)");
            } else if (!StringUtils.hasText(secretoEfectivo)) {
                log.warn(
                        "pagos-comprobante: enabled=true pero falta secreto interno efectivo; "
                                + "debe coincidir con patiperro.pagos.interno.secret (p. ej. PAGOS_INTERNO_SECRET o "
                                + "RESERVA_INTEGRACION_PAGOS_COMPROBANTE_INTERNO_SECRET)");
            }
            return;
        }
        log.info(
                "pagos-comprobante: integración activa (tras COMMIT → POST interno en pagos-service). "
                        + "Correo al tutor depende de pagos-service (PAGOS_INTEGRACION_NOTIFICATION_ENABLED) y notification-service.");
    }

    public boolean isEnabled() {
        return enabled && restClient != null;
    }

    /**
     * @return {@code true} si pagos-service respondió 2xx
     */
    public boolean generarYEnviarResumen(Integer idReserva, boolean forceEnviarCorreo) {
        if (!isEnabled() || idReserva == null) {
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva.longValue());
        body.put("forceEnviarCorreo", forceEnviarCorreo);
        try {
            var entity = restClient.post()
                    .uri(URI_GENERAR_ENVIAR)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION, correlationValue(idReserva))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return entity.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            log.warn("Comprobante post-pago: pagos-service HTTP {} (idReserva={})", e.getStatusCode().value(), idReserva);
            return false;
        } catch (RestClientException e) {
            log.warn("Comprobante post-pago: error de red (idReserva={})", idReserva, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("Comprobante post-pago: error inesperado (idReserva={})", idReserva, e);
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

    private static String resolveNonBlank(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred;
        }
        return fallback != null ? fallback : "";
    }

    private static long resolveLong(String raw, long fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * Timeout de lectura: si está definido explícitamente en comprobante.* se respeta (clamp en factory).
     * Si se hereda de reembolso y éste es muy alto, se acota a {@link #READ_TIMEOUT_MS_FALLBACK_CAP}.
     */
    private static long resolveReadTimeoutMs(
            PagosComprobanteIntegracionProperties comprobanteProps,
            PagosReembolsoIntegracionProperties reembolsoProps) {
        long inherited = reembolsoProps.getReadTimeoutMs();
        if (StringUtils.hasText(comprobanteProps.getReadTimeoutMs())) {
            return resolveLong(comprobanteProps.getReadTimeoutMs(), inherited);
        }
        long effective = inherited;
        if (effective > READ_TIMEOUT_MS_FALLBACK_CAP) {
            effective = READ_TIMEOUT_MS_FALLBACK_CAP;
        }
        return Math.max(1_000L, effective);
    }

    private static String correlationValue(Integer idReserva) {
        return "reserva-" + idReserva;
    }

    private static SimpleClientHttpRequestFactory requestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(connectTimeoutMs, 120_000L))));
        factory.setReadTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(readTimeoutMs, 600_000L))));
        return factory;
    }
}
