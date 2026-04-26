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

/**
 * Cliente hacia {@code notification-service} ({@code POST /internal/paseo/inicio}).
 * Deshabilitado por defecto: sin base-url o sin secreto no hace red.
 * El valor de secreto debe coincidir con {@code patiperro.notification.interno.secret} en notification-service.
 */
@Component
public class NotificacionPaseoIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPaseoIntegracionClient.class);

    /** Misma cabecera que {@code InternalPaseoController} en notification-service. */
    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_INICIO_PASEO = "/internal/paseo/inicio";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionPaseoIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.notificacion.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.notificacion.base-url:}") String baseUrl,
            @Value("${patiperro.reserva.integracion.notificacion.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * Avisa a notification-service del inicio de paseo. No propaga excepciones.
     */
    public void notificarInicioPaseo(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.info("Notificación paseo: integración habilitada pero sin base-url; no se invoca (reserva={})",
                        idReserva);
            } else if (enabled && restClient != null && !StringUtils.hasText(internoSecret)) {
                log.info("Notificación paseo: integración habilitada pero sin patiperro.reserva.integracion.notificacion.interno.secret; no se invoca (reserva={})",
                        idReserva);
            }
            return;
        }
        try {
            String json = "{\"idReserva\":" + idReserva + "}";
            restClient
                    .post()
                    .uri(URI_INICIO_PASEO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Notificación paseo: respuesta no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Notificación paseo: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Notificación paseo: error inesperado para reserva {}", idReserva, e);
        }
    }
}
