package com.patiperro.pagos.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.checkout.dto.MercadoPagoPreferenceCreatedDto;
import com.patiperro.pagos.config.MercadoPagoRestClientConfig;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta el estado real del pago en Mercado Pago (fuente de verdad tras webhook/IPN).
 */
@Component
public class MercadoPagoApiClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoApiClient.class);

    private static final Pattern PAYMENTS_PATH = Pattern.compile("/payments/([^/?#]+)");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;

    public MercadoPagoApiClient(
            @Qualifier(MercadoPagoRestClientConfig.MERCADOPAGO_REST_CLIENT_BEAN) RestClient mercadoPagoRestClient,
            ObjectMapper objectMapper,
            @Value("${patiperro.mercadopago.access-token:}") String accessToken) {
        this.restClient = mercadoPagoRestClient;
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    /**
     * Crea una preferencia Checkout Pro (redirección a init_point / sandbox_init_point).
     */
    public Optional<MercadoPagoPreferenceCreatedDto> crearPreferenciaCheckoutPro(
            String itemTitle,
            String currencyId,
            long unitPrice,
            String externalReference,
            String notificationUrl,
            String successUrl,
            String failureUrl,
            String pendingUrl) {
        if (!StringUtils.hasText(accessToken)) {
            log.warn("Mercado Pago: access-token no configurado; no se crea preferencia Checkout Pro");
            return Optional.empty();
        }
        if (!StringUtils.hasText(externalReference)) {
            return Optional.empty();
        }
        try {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", itemTitle);
            item.put("quantity", 1);
            item.put("currency_id", currencyId);
            item.put("unit_price", unitPrice);

            Map<String, String> backUrls = new LinkedHashMap<>();
            backUrls.put("success", successUrl);
            backUrls.put("failure", failureUrl);
            backUrls.put("pending", pendingUrl);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", List.of(item));
            body.put("external_reference", externalReference);
            if (StringUtils.hasText(notificationUrl)) {
                body.put("notification_url", notificationUrl);
            }
            body.put("back_urls", backUrls);
            // Con auto_return=approved, MP exige back_urls.success válida (típicamente HTTPS). URLs http://localhost
            // provocan 400 invalid_auto_return; sin auto_return el checkout sigue funcionando (el usuario vuelve con el link de MP).
            boolean successHttps = StringUtils.hasText(successUrl)
                    && successUrl.trim().regionMatches(true, 0, "https://", 0, 8);
            if (successHttps) {
                body.put("auto_return", "approved");
            }

            String jsonBody = objectMapper.writeValueAsString(body);
            String responseBody = restClient.post()
                    .uri("/checkout/preferences")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(responseBody)) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String prefId = textOrNull(root.get("id"));
            String init = textOrNull(root.get("init_point"));
            String sandbox = textOrNull(root.get("sandbox_init_point"));
            if (!StringUtils.hasText(init) && !StringUtils.hasText(sandbox)) {
                return Optional.empty();
            }
            return Optional.of(new MercadoPagoPreferenceCreatedDto(prefId, init, sandbox));
        } catch (RestClientResponseException e) {
            log.warn("Mercado Pago: crear preferencia respondió {} ({})", e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Mercado Pago: error de red creando preferencia Checkout Pro", e);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Mercado Pago: JSON inválido al crear preferencia", e);
            return Optional.empty();
        }
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return null;
        }
        String t = n.asText();
        return StringUtils.hasText(t) ? t.trim() : null;
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
            logRespuestaHttp(paymentId, e.getStatusCode().value(), e);
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Mercado Pago: error de red consultando pago {}", paymentId, e);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Mercado Pago: respuesta JSON inválida para pago {}", paymentId, e);
            return Optional.empty();
        }
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
        try {
            String key = sanitizeIdempotencyKey(idempotencyKey);
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
            logRefundHttp(paymentId, e.getStatusCode().value(), e);
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Mercado Pago: error de red creando reembolso para pago {}", paymentId, e);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Mercado Pago: respuesta JSON inválida al crear reembolso (pago={})", paymentId, e);
            return Optional.empty();
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
