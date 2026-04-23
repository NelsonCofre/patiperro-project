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
 * Cliente HTTP hacia un futuro módulo de tracking (ruta placeholder).
 * Con {@code patiperro.reserva.integracion.tracking.enabled=false} o sin base-url no hace red.
 */
@Component
public class TrackingIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(TrackingIntegracionClient.class);
    private static final String URI_SESIONES = "/internal/tracking/sesiones";

    private final RestClient restClient;
    private final boolean enabled;

    public TrackingIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.tracking.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.tracking.base-url:}") String baseUrl) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
    }

    public boolean isEnabled() {
        return enabled && restClient != null;
    }

    /**
     * Notifica al servicio de tracking el inicio de paseo (placeholder).
     * No lanza: errores de red o de respuesta se registran y se ignoran.
     */
    public void crearSesionInicioPaseo(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        if (!isEnabled()) {
            if (enabled && restClient == null) {
                log.info("Tracking integración habilitada pero sin base-url; no se invoca (reserva={})", idReserva);
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
            log.warn("Tracking: respuesta no exitosa (reserva={}, status={})", idReserva, e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Tracking: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Tracking: error inesperado para reserva {}", idReserva, e);
        }
    }
}
