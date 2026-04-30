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

import java.util.Map;

/**
 * Notifica a {@code reserva-service} que un pago de Mercado Pago fue aprobado (endpoint interno).
 */
@Component
public class ReservaPagosIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaPagosIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_PAGO_APROBADO = "/api/reserva/interno/pagos/mercadopago/pago-aprobado";

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
}
