package com.patiperro.reserva.support;

import com.patiperro.reserva.config.properties.PagosReembolsoIntegracionProperties;
import com.patiperro.reserva.dto.integracion.ReembolsoFlagsPagosDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final String URI_FLAGS = "/api/pagos/interno/reembolso/flags-reserva/{idReserva}";
    private static final String URI_CANDIDATOS_CORREO = "/api/pagos/interno/reembolso/candidatos-correo";
    private static final String URI_MARCAR_CORREO = "/api/pagos/interno/reembolso/marcar-correo-reembolso-enviado";

    private static final int BODY_DEBUG_MAX_CHARS = 512;

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PagosReembolsoIntegracionClient(
            RestClient.Builder restClientBuilder, PagosReembolsoIntegracionProperties props) {
        this.enabled = props.isEnabled();
        String base = normalizeBaseUrl(props.getBaseUrl());
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(props.getConnectTimeoutMs(), props.getReadTimeoutMs()))
                        .baseUrl(base)
                        .build();
        String rawSecret = props.getInterno().getSecret();
        this.internoSecret = rawSecret != null ? rawSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    public Optional<ReembolsoFlagsPagosDto> consultarFlagsReembolso(Integer idReserva) {
        if (!isEnabled() || idReserva == null) {
            return Optional.empty();
        }
        try {
            ReembolsoFlagsPagosDto dto = restClient.get()
                    .uri(URI_FLAGS, idReserva)
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(ReembolsoFlagsPagosDto.class);
            return Optional.ofNullable(dto);
        } catch (RestClientResponseException e) {
            log.warn("Reembolso flags pagos: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode());
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Reembolso flags pagos: llamada no completada (reserva={})", idReserva, e);
            return Optional.empty();
        }
    }

    public List<Integer> listarCandidatosCorreoReembolso(int size) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        int lim = Math.min(Math.max(size, 1), 200);
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(URI_CANDIDATOS_CORREO).queryParam("size", lim).build())
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Integer>>() {
                    });
        } catch (RestClientResponseException e) {
            log.warn("Reembolso candidatos correo: respuesta no exitosa (status={})", e.getStatusCode());
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Reembolso candidatos correo: llamada no completada", e);
            return Collections.emptyList();
        }
    }

    public void marcarCorreoReembolsoEnviadoEnPagos(Integer idReserva) {
        if (!isEnabled() || idReserva == null) {
            return;
        }
        try {
            restClient.post()
                    .uri(URI_MARCAR_CORREO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("idReserva", idReserva))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Reembolso marcar correo pagos: respuesta no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode());
        } catch (RestClientException e) {
            log.warn("Reembolso marcar correo pagos: llamada no completada (reserva={})", idReserva, e);
        }
    }

    /**
     * @return código HTTP de pagos-service (p. ej. 204 éxito, 502 fallo MP), o {@code 0} si no hubo llamada HTTP
     *         (integración deshabilitada / sin configuración)
     */
    public int solicitarReembolsoTotal(Integer idReserva, String mpPaymentId) {
        if (idReserva == null && !StringUtils.hasText(mpPaymentId)) {
            return 400;
        }
        if (!isEnabled()) {
            log.debug("Integración reembolso pagos deshabilitada o sin config; sin llamada HTTP (reserva={})", idReserva);
            return 0;
        }
        String trimmedPaymentId = StringUtils.hasText(mpPaymentId) ? mpPaymentId.trim() : "";
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            if (StringUtils.hasText(trimmedPaymentId)) {
                body.put("mpPaymentId", trimmedPaymentId);
            }
            if (idReserva != null) {
                body.put("idReserva", idReserva);
            }
            if (body.isEmpty()) {
                return 400;
            }
            var reqBuilder = restClient.post()
                    .uri(URI_REEMBOLSO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            if (StringUtils.hasText(trimmedPaymentId)) {
                String idempotencyKey = idempotencyKeyReembolso(idReserva, trimmedPaymentId);
                reqBuilder = reqBuilder.header(HEADER_IDEMPOTENCY_KEY, idempotencyKey);
            }
            var req = reqBuilder;
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
