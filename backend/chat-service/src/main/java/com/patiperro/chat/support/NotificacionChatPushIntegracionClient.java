package com.patiperro.chat.support;

import com.patiperro.chat.dto.ChatNuevoMensajePushIntegracionRequest;
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
 * Cliente hacia {@code notification-service} ({@code POST /internal/chat/nuevo-mensaje}).
 * Deshabilitado por defecto. El secreto debe coincidir con
 * {@code patiperro.notification.interno.secret} en notification-service.
 */
@Component
public class NotificacionChatPushIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionChatPushIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_NUEVO_MENSAJE = "/internal/chat/nuevo-mensaje";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public NotificacionChatPushIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.chat.integracion.notification.enabled:false}") boolean enabled,
            @Value("${patiperro.chat.integracion.notification.base-url:http://localhost:8086}") String baseUrl,
            @Value("${patiperro.chat.integracion.notification.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /** Best-effort: no propaga excepciones al WebSocket. */
    public void notificarNuevoMensaje(ChatNuevoMensajePushIntegracionRequest payload) {
        if (payload == null || payload.idUsuarioDestino() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri(URI_NUEVO_MENSAJE)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Push chat: notification HTTP {} (reserva={}, destino={})",
                    e.getStatusCode(), payload.idReserva(), payload.idUsuarioDestino());
        } catch (RestClientException e) {
            log.warn("Push chat: llamada no completada (reserva={}, destino={})",
                    payload.idReserva(), payload.idUsuarioDestino(), e);
        } catch (RuntimeException e) {
            log.warn("Push chat: error inesperado (reserva={}, destino={})",
                    payload.idReserva(), payload.idUsuarioDestino(), e);
        }
    }
}
