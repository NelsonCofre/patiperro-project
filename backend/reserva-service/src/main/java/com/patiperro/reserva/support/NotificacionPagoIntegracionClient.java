package com.patiperro.reserva.support;

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

/**
 * Cliente hacia {@code notification-service} ({@code POST /internal/pagos/confirmado}).
 * Deshabilitado por defecto: sin base-url o sin secreto no hace red.
 */
@Component
public class NotificacionPagoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPagoIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_CONFIRMADO = "/internal/pagos/confirmado";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionPagoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.notificacion-pago.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.notificacion-pago.base-url:}") String baseUrl,
            @Value("${patiperro.reserva.integracion.notificacion-pago.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * Notifica pago confirmado al notification-service (correo Brevo al paseador si hay {@code emailDestino}).
     */
    public void notificarPagoConfirmado(Integer idReserva, Integer idPaseador, String emailDestino) {
        if (idReserva == null) {
            return;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.debug("Notificación pago: integración habilitada pero sin base-url; omitido (reserva={})", idReserva);
            } else if (enabled && restClient != null && !StringUtils.hasText(internoSecret)) {
                log.debug("Notificación pago: falta patiperro.reserva.integracion.notificacion-pago.interno.secret; omitido");
            }
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        if (idPaseador != null) {
            body.put("idPaseador", idPaseador);
        }
        if (StringUtils.hasText(emailDestino)) {
            body.put("emailDestino", emailDestino.trim());
        }
        try {
            restClient.post()
                    .uri(URI_CONFIRMADO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Notificación pago: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Notificación pago: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Notificación pago: error inesperado para reserva {}", idReserva, e);
        }
    }
}
