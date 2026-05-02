package com.patiperro.pagos.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notifica a {@code reserva-service} que un pago de Mercado Pago fue aprobado (endpoint interno).
 */
@Component
public class ReservaPagosIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaPagosIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /** Misma cabecera que {@code InternalPagosController.HEADER_CORRELATION_ID} en reserva-service. */
    public static final String HEADER_CORRELATION_ID = "X-Patiperro-Correlation-Id";

    private static final String URI_PAGO_APROBADO = "/api/reserva/interno/pagos/mercadopago/pago-aprobado";

    private static final String URI_PAGO_NO_APROBADO = "/api/reserva/interno/pagos/mercadopago/pago-no-aprobado";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public ReservaPagosIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.reserva.enabled:true}") boolean enabled,
            @Value("${patiperro.pagos.integracion.reserva.base-url:http://localhost:8085}") String baseUrl,
            @Value("${patiperro.pagos.integracion.reserva.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public void notificarPagoAprobado(Integer idReserva, String mpPaymentId) {
        if (idReserva == null || !StringUtils.hasText(mpPaymentId)) {
            log.warn("Integración reserva: idReserva o mpPaymentId vacío; no se invoca");
            return;
        }
        if (!enabled || restClient == null) {
            log.debug("Integración reserva deshabilitada o sin base-url; omitido (reserva={})", idReserva);
            return;
        }
        if (!StringUtils.hasText(internoSecret)) {
            log.warn("Integración reserva: falta patiperro.pagos.integracion.reserva.interno.secret; omitido (reserva={})",
                    idReserva);
            return;
        }
        try {
            restClient.post()
                    .uri(URI_PAGO_APROBADO)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("idReserva", idReserva, "mpPaymentId", mpPaymentId.trim()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Integración reserva: respuesta no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Integración reserva: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Integración reserva: error inesperado para reserva {}", idReserva, e);
        }
    }

    /**
     * Informa a {@code reserva-service} un pago Mercado Pago que no quedó en {@code approved}.
     */
    public void notificarPagoNoAprobado(Integer idReserva, String mpPaymentId, String mpStatus, String mpStatusDetail) {
        if (idReserva == null || !StringUtils.hasText(mpPaymentId) || !StringUtils.hasText(mpStatus)) {
            log.warn("Integración reserva: idReserva, mpPaymentId o mpStatus vacío; no se invoca pago-no-aprobado");
            return;
        }
        if (!enabled || restClient == null) {
            log.debug("Integración reserva deshabilitada o sin base-url; omitido pago-no-aprobado (reserva={})", idReserva);
            return;
        }
        if (!StringUtils.hasText(internoSecret)) {
            log.warn("Integración reserva: falta patiperro.pagos.integracion.reserva.interno.secret; omitido (reserva={})",
                    idReserva);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("mpPaymentId", mpPaymentId.trim());
        body.put("mpStatus", mpStatus.trim());
        if (StringUtils.hasText(mpStatusDetail)) {
            body.put("mpStatusDetail", mpStatusDetail.trim());
        }
        try {
            restClient.post()
                    .uri(URI_PAGO_NO_APROBADO)
                    .header(HEADER_INTERNO, internoSecret)
                    .header(HEADER_CORRELATION_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Integración reserva: pago-no-aprobado respuesta no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Integración reserva: pago-no-aprobado llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Integración reserva: pago-no-aprobado error inesperado para reserva {}", idReserva, e);
        }
    }
}
