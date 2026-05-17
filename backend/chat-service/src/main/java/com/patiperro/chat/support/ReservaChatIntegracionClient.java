package com.patiperro.chat.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Participantes de una reserva vía {@code GET /api/reserva/interno/{id}/comprobante}
 * (mismo patrón que pagos-service). No propaga excepciones al llamador.
 */
@Component
public class ReservaChatIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaChatIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public ReservaChatIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.chat.integracion.reserva.enabled:true}") boolean enabled,
            @Value("${patiperro.chat.integracion.reserva.base-url:http://localhost:8090}") String baseUrl,
            @Value("${patiperro.chat.integracion.reserva.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * {@code null} si la integración está deshabilitada, la reserva no existe o falla la llamada HTTP.
     */
    public ReservaParticipantesDto obtenerParticipantes(Integer idReserva) {
        if (idReserva == null || !isEnabled()) {
            return null;
        }
        try {
            return restClient
                    .get()
                    .uri("/api/reserva/interno/{idReserva}/comprobante", idReserva)
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(ReservaParticipantesDto.class);
        } catch (RestClientResponseException e) {
            log.warn("Participantes reserva: HTTP {} (reserva={})", e.getStatusCode(), idReserva);
            return null;
        } catch (RestClientException e) {
            log.warn("Participantes reserva: llamada fallida (reserva={})", idReserva, e);
            return null;
        } catch (RuntimeException e) {
            log.warn("Participantes reserva: error inesperado (reserva={})", idReserva, e);
            return null;
        }
    }
}
