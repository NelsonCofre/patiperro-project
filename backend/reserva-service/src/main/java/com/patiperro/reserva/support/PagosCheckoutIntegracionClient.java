package com.patiperro.reserva.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.reserva.config.properties.PagosCheckoutIntegracionProperties;
import com.patiperro.reserva.dto.TutorCheckoutPreferenciaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Crea preferencia Mercado Pago Checkout Pro en pagos-service (endpoint interno).
 * <p>Configuración: {@code patiperro.reserva.integracion.pagos-checkout.*}; si no se define,
 * hereda de {@code patiperro.reserva.integracion.pagos-reembolso.*} (ver {@code application.properties}).</p>
 */
@Component
public class PagosCheckoutIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(PagosCheckoutIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Misma convención que en pagos-service ({@code ReservaPagosIntegracionClient}) para trazas cruzadas. */
    public static final String HEADER_CORRELATION_ID = "X-Patiperro-Correlation-Id";

    private static final String URI_CHECKOUT_PREFERENCIA = "/api/pagos/interno/mercadopago/checkout/preferencia";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String internoSecret;

    public PagosCheckoutIntegracionClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            PagosCheckoutIntegracionProperties props) {
        this.objectMapper = objectMapper;
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

    /**
     * @param idempotencyKey opcional; si viene vacío se genera uno por llamada
     */
    public Optional<TutorCheckoutPreferenciaResponseDTO> crearPreferenciaCheckout(
            Integer idReserva,
            BigDecimal montoTotal,
            String tituloItem,
            String idempotencyKey) {
        if (idReserva == null || montoTotal == null) {
            log.warn("Checkout pagos: idReserva o montoTotal null");
            return Optional.empty();
        }
        if (!isEnabled()) {
            log.warn("Checkout pagos: integración deshabilitada o sin configuración");
            return Optional.empty();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("montoTotal", montoTotal);
        if (StringUtils.hasText(tituloItem)) {
            body.put("tituloItem", tituloItem.trim());
        }
        String key = StringUtils.hasText(idempotencyKey)
                ? idempotencyKey.trim()
                : "patiperro-checkout-reserva-" + idReserva + "-" + UUID.randomUUID();
        if (key.length() > 255) {
            key = key.substring(0, 255);
        }
        try {
            String correlationId = "checkout-reserva-" + idReserva + "-" + UUID.randomUUID();
            String raw = restClient.post()
                    .uri(URI_CHECKOUT_PREFERENCIA)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_IDEMPOTENCY_KEY, key)
                    .header(HEADER_CORRELATION_ID, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(raw)) {
                return Optional.empty();
            }
            TutorCheckoutPreferenciaResponseDTO dto = objectMapper.readValue(raw, TutorCheckoutPreferenciaResponseDTO.class);
            return Optional.ofNullable(dto);
        } catch (RestClientResponseException e) {
            log.warn("Checkout pagos: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode());
            try {
                String err = e.getResponseBodyAsString();
                if (StringUtils.hasText(err) && err.length() < 512) {
                    log.warn("Checkout pagos: cuerpo error: {}", err);
                }
            } catch (RuntimeException ignored) {
                // ignore
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Checkout pagos: llamada no completada (reserva={})", idReserva, e);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Checkout pagos: error parseando respuesta (reserva={})", idReserva, e);
            return Optional.empty();
        }
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

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String base = baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
