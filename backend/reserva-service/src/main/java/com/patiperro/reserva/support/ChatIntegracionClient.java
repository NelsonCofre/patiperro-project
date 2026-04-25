package com.patiperro.reserva.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP hacia un futuro módulo de chat (ruta placeholder).
 * Con {@code patiperro.reserva.integracion.chat.enabled=false} o sin base-url no hace red.
 */
@Component
public class ChatIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ChatIntegracionClient.class);
    private static final String URI_SESIONES = "/internal/chat/sesiones";

    private final RestClient restClient;
    private final boolean enabled;

    public ChatIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.chat.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.chat.base-url:}") String baseUrl) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
    }

    public boolean isEnabled() {
        return enabled && restClient != null;
    }

    /**
     * Notifica al servicio de chat el inicio de paseo / sesión (placeholder).
     * No lanza: errores de red o de respuesta se registran y se ignoran.
     */
    public void crearSesionInicioPaseo(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.info("Chat integración habilitada pero sin base-url; no se invoca (reserva={})", idReserva);
            }
            return;
        }
        try {
            String json = "{\"idReserva\":" + idReserva + "}";
            restClient
                    .post()
                    .uri(URI_SESIONES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Chat: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Chat: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Chat: error inesperado para reserva {}", idReserva, e);
        }
    }
}
