package com.patiperro.pagos.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.config.MercadoPagoRestClientConfig;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
