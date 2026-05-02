package com.patiperro.pagos.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.config.MercadoPagoRestClientConfig;
import com.patiperro.pagos.config.MercadoPagoRetryProperties;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.dto.MercadoPagoPreferenceResponseDto;
import com.patiperro.pagos.dto.MercadoPagoRefundResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta el estado real del pago en Mercado Pago (fuente de verdad tras webhook/IPN).
 * Reintenta automáticamente ante rate limit (429), errores 5xx y fallos de red.
 */
@Component
public class MercadoPagoApiClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoApiClient.class);

    private static final Pattern PAYMENTS_PATH = Pattern.compile("/payments/([^/?#]+)");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final int retryMaxAttempts;
    private final long retryInitialDelayMs;
    private final long retryMaxDelayMs;

    public MercadoPagoApiClient(
            @Qualifier(MercadoPagoRestClientConfig.MERCADOPAGO_REST_CLIENT_BEAN) RestClient mercadoPagoRestClient,
            ObjectMapper objectMapper,
            @Value("${patiperro.mercadopago.access-token:}") String accessToken,
            MercadoPagoRetryProperties retryProperties) {
        this.restClient = mercadoPagoRestClient;
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.retryMaxAttempts = Math.max(1, retryProperties.getMaxAttempts());
        this.retryInitialDelayMs = Math.max(0L, retryProperties.getInitialDelayMs());
        this.retryMaxDelayMs = Math.max(this.retryInitialDelayMs, retryProperties.getMaxDelayMs());
    }

    public Optional<MercadoPagoPaymentDto> obtenerPago(String paymentIdRaw) {
        String paymentId = normalizarPaymentId(paymentIdRaw);
        if (!StringUtils.hasText(paymentId)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(accessToken)) {
            log.warn("Mercado Pago: patiperro.mercadopago.access-token no configurado; no se consulta el pago {}", paymentId);
            return Optional.empty();
        }

        long delayMs = retryInitialDelayMs;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                String body = restClient.get()
                        .uri("/v1/payments/{id}", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(String.class);
                if (!StringUtils.hasText(body)) {
                    return Optional.empty();
                }
                MercadoPagoPaymentDto dto = objectMapper.readValue(body, MercadoPagoPaymentDto.class);
                return Optional.ofNullable(dto);
            } catch (RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if (!isTransientHttpStatus(code) || attempt >= retryMaxAttempts) {
                    logRespuestaHttp(paymentId, code, e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: GET payment id={} respondió {} — reintento {}/{}",
                        paymentId, code, attempt, retryMaxAttempts);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (RestClientException e) {
                if (attempt >= retryMaxAttempts) {
                    log.warn("Mercado Pago: error de red consultando pago {}", paymentId, e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: error de red consultando pago {} — reintento {}/{}",
                        paymentId, attempt, retryMaxAttempts, e);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (JsonProcessingException e) {
                log.warn("Mercado Pago: respuesta JSON inválida para pago {}", paymentId, e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void logRespuestaHttp(String paymentId, int code, RestClientResponseException e) {
        if (code == 404) {
            log.warn("Mercado Pago: pago no encontrado (id={})", paymentId);
            return;
        }
        if (code == 401 || code == 403) {
            log.warn("Mercado Pago: token inválido o sin permiso al consultar pago {} (status={})", paymentId, code);
            return;
        }
        if (code == 429) {
            log.warn("Mercado Pago: rate limit al consultar pago {} — conviene reintentar más tarde", paymentId);
            return;
        }
        if (code >= 500 && code <= 599) {
            log.warn("Mercado Pago: error del servidor ({}) al consultar pago {}", code, paymentId);
            return;
        }
        log.warn("Mercado Pago: GET payment falló (id={}, status={})", paymentId, code, e);
    }

    /**
     * Reembolso total del pago (cuerpo vacío {@code {}} según API de Mercado Pago).
     * Mercado Pago usa {@code Idempotency-Key} para deduplicar reintentos del mismo reembolso lógico.
     *
     * @param idempotencyKey clave estable por operación (reintentos deben reutilizarla); si viene vacía no se envía cabecera
     * @return respuesta parseada en éxito; vacío si error de red, JSON inválido o respuesta HTTP no 2xx
     */
    public Optional<MercadoPagoRefundResponseDto> crearReembolsoTotal(String paymentIdRaw, String idempotencyKey) {
        String paymentId = normalizarPaymentId(paymentIdRaw);
        if (!StringUtils.hasText(paymentId)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(accessToken)) {
            log.warn("Mercado Pago: access-token no configurado; no se crea reembolso para pago {}", paymentId);
            return Optional.empty();
        }

        String key = sanitizeIdempotencyKey(idempotencyKey);
        long delayMs = retryInitialDelayMs;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                RequestBodySpec post = restClient.post()
                        .uri("/v1/payments/{id}/refunds", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON);
                if (StringUtils.hasText(key)) {
                    post = post.header("Idempotency-Key", key);
                }
                String body = post.body("{}")
                        .retrieve()
                        .body(String.class);
                if (!StringUtils.hasText(body)) {
                    return Optional.empty();
                }
                MercadoPagoRefundResponseDto dto = objectMapper.readValue(body, MercadoPagoRefundResponseDto.class);
                return Optional.ofNullable(dto);
            } catch (RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if (!isTransientHttpStatus(code) || attempt >= retryMaxAttempts) {
                    logRefundHttp(paymentId, code, e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: POST refund pago={} respondió {} — reintento {}/{}",
                        paymentId, code, attempt, retryMaxAttempts);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (RestClientException e) {
                if (attempt >= retryMaxAttempts) {
                    log.warn("Mercado Pago: error de red creando reembolso para pago {}", paymentId, e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: error de red creando reembolso para pago {} — reintento {}/{}",
                        paymentId, attempt, retryMaxAttempts, e);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (JsonProcessingException e) {
                log.warn("Mercado Pago: respuesta JSON inválida al crear reembolso (pago={})", paymentId, e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Crea una preferencia Checkout Pro ({@code POST /checkout/preferences}).
     *
     * @param preferenceBody cuerpo JSON lógico (se serializa con {@link ObjectMapper})
     * @param idempotencyKey opcional; recomendado para deduplicar el mismo intento de checkout
     */
    public Optional<MercadoPagoPreferenceResponseDto> crearPreferenciaCheckout(
            Map<String, Object> preferenceBody,
            String idempotencyKeyRaw) {
        if (preferenceBody == null || preferenceBody.isEmpty()) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(accessToken)) {
            log.warn("Mercado Pago: access-token no configurado; no se crea preferencia checkout");
            return Optional.empty();
        }

        final String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(preferenceBody);
        } catch (JsonProcessingException e) {
            log.warn("Mercado Pago: no se serializó el body de preferencia checkout", e);
            return Optional.empty();
        }

        String key = sanitizeIdempotencyKey(idempotencyKeyRaw);
        long delayMs = retryInitialDelayMs;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                RequestBodySpec post = restClient.post()
                        .uri("/checkout/preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON);
                if (StringUtils.hasText(key)) {
                    post = post.header("Idempotency-Key", key);
                }
                String body = post.body(jsonBody)
                        .retrieve()
                        .body(String.class);
                if (!StringUtils.hasText(body)) {
                    return Optional.empty();
                }
                MercadoPagoPreferenceResponseDto dto = objectMapper.readValue(body, MercadoPagoPreferenceResponseDto.class);
                return Optional.ofNullable(dto);
            } catch (RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if (!isTransientHttpStatus(code) || attempt >= retryMaxAttempts) {
                    logPreferenceHttp(code, e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: POST checkout/preferences respondió {} — reintento {}/{}",
                        code, attempt, retryMaxAttempts);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (RestClientException e) {
                if (attempt >= retryMaxAttempts) {
                    log.warn("Mercado Pago: error de red creando preferencia checkout", e);
                    return Optional.empty();
                }
                log.warn("Mercado Pago: error de red creando preferencia checkout — reintento {}/{}",
                        attempt, retryMaxAttempts, e);
                sleepBackoff(delayMs);
                delayMs = nextBackoffDelay(delayMs);
            } catch (JsonProcessingException e) {
                log.warn("Mercado Pago: respuesta JSON inválida al crear preferencia checkout", e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void logPreferenceHttp(int code, RestClientResponseException e) {
        if (code == 401 || code == 403) {
            log.warn("Mercado Pago: token inválido o sin permiso al crear preferencia checkout (status={})", code);
            return;
        }
        if (code == 429) {
            log.warn("Mercado Pago: rate limit al crear preferencia checkout");
            return;
        }
        if (code >= 500 && code <= 599) {
            log.warn("Mercado Pago: error del servidor ({}) al crear preferencia checkout", code);
            return;
        }
        log.warn("Mercado Pago: POST checkout/preferences falló (status={})", code, e);
    }

    /**
     * HTTP considerados transitorios: rate limit y errores del lado servidor MP/proxy.
     */
    private static boolean isTransientHttpStatus(int code) {
        if (code == 429) {
            return true;
        }
        return code >= 500 && code <= 599;
    }

    private long nextBackoffDelay(long currentDelayMs) {
        long base = currentDelayMs > 0 ? currentDelayMs : retryInitialDelayMs;
        if (base <= 0) {
            base = 250L;
        }
        long doubled = base > Long.MAX_VALUE / 2 ? retryMaxDelayMs : base * 2;
        return Math.min(doubled, retryMaxDelayMs);
    }

    private void sleepBackoff(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logRefundHttp(String paymentId, int code, RestClientResponseException e) {
        if (code == 404) {
            log.warn("Mercado Pago: pago no encontrado al reembolsar (id={})", paymentId);
            return;
        }
        if (code == 401 || code == 403) {
            log.warn("Mercado Pago: token inválido o sin permiso al reembolsar pago {} (status={})", paymentId, code);
            return;
        }
        if (code == 429) {
            log.warn("Mercado Pago: rate limit al reembolsar pago {}", paymentId);
            return;
        }
        if (code >= 500 && code <= 599) {
            log.warn("Mercado Pago: error del servidor ({}) al reembolsar pago {}", code, paymentId);
            return;
        }
        log.info("Mercado Pago: POST refund respondió {} para pago {} (puede ser duplicado o regla MP)", code, paymentId);
    }

    /** Límite conservador de longitud para cabeceras de idempotencia en APIs HTTP. */
    private static final int IDEMPOTENCY_KEY_MAX_LEN = 255;

    public static String sanitizeIdempotencyKey(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = raw.trim().replaceAll("[\\r\\n]", "");
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > IDEMPOTENCY_KEY_MAX_LEN) {
            return t.substring(0, IDEMPOTENCY_KEY_MAX_LEN);
        }
        return t;
    }

    /**
     * Acepta id numérico o URLs que contengan {@code /payments/{id}}.
     */
    public static String normalizarPaymentId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim();
        Matcher m = PAYMENTS_PATH.matcher(s);
        if (m.find()) {
            return m.group(1).trim();
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q).trim();
        }
        return s;
    }
}
