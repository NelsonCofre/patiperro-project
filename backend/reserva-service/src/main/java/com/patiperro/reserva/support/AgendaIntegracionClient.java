package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.AgendaBloqueResumenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

@Component
public class AgendaIntegracionClient {

    private static final String URI_BLOQUE_POR_ID = "/api/agenda/bloques/{id}";
    private static final String URI_BLOQUE_POR_ID_INTERNO = "/api/agenda/interno/bloques/{id}";
    private static final String HEADER_AGENDA_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final boolean enabled;
    private final String agendaInternoSecret;

    public AgendaIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.agenda-integracion.enabled:true}") boolean enabled,
            @Value("${patiperro.reserva.agenda-integracion.base-url:}") String baseUrl,
            @Value("${patiperro.reserva.agenda-interno.secret:}") String agendaInternoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.agendaInternoSecret = agendaInternoSecret != null ? agendaInternoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null;
    }

    /**
     * Obtiene un bloque de agenda por id (misma ruta que agenda-service).
     * Requiere JWT porque {@code GET /api/agenda/bloques/{id}} está protegido.
     */
    /**
     * Lista todos los bloques del usuario (paseador). Requiere JWT.
     */
    public List<AgendaBloqueReservaClientDTO> listarBloquesPorUsuario(Integer idUsuario, String rawJwt) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para listar bloques en agenda-service");
        }
        try {
            return restClient.get()
                    .uri("/api/agenda/bloques/usuario/{idUsuario}", idUsuario)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AgendaBloqueReservaClientDTO>>() {
                    });
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    public AgendaBloqueReservaClientDTO obtenerBloquePorId(Integer idAgendaBloque, String rawJwt) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Integración con agenda-service deshabilitada o sin base-url; no se puede obtener el bloque");
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para consultar bloque en agenda-service");
        }
        try {
            return restClient
                    .get()
                    .uri(URI_BLOQUE_POR_ID, idAgendaBloque)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .body(AgendaBloqueReservaClientDTO.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un bloque de agenda por id mediante endpoint interno (sin JWT).
     * Requiere {@code patiperro.reserva.agenda-interno.secret}.
     */
    public AgendaBloqueReservaClientDTO obtenerBloquePorIdInterno(Integer idAgendaBloque) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "Integración con agenda-service deshabilitada o sin base-url; no se puede obtener el bloque");
        }
        if (!StringUtils.hasText(agendaInternoSecret)) {
            throw new IllegalStateException(
                    "Falta patiperro.reserva.agenda-interno.secret para consultar bloque interno en agenda-service");
        }
        try {
            return restClient
                    .get()
                    .uri(URI_BLOQUE_POR_ID_INTERNO, idAgendaBloque)
                    .header(HEADER_AGENDA_INTERNO, agendaInternoSecret)
                    .retrieve()
                    .body(AgendaBloqueReservaClientDTO.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service (interno) respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    public void marcarBloqueReservado(Integer idAgendaBloque, String rawJwt) {
        if (!isEnabled()) {
            return;
        }
        patchBloque(idAgendaBloque, rawJwt, "/api/agenda/bloques/{id}/marcar-reservado");
    }

    public void marcarBloqueDisponible(Integer idAgendaBloque, String rawJwt) {
        if (!isEnabled()) {
            return;
        }
        patchBloque(idAgendaBloque, rawJwt, "/api/agenda/bloques/{id}/marcar-disponible");
    }

    /**
     * Libera el bloque tras reglas validadas en reserva-service (p. ej. cancelación
     * por tutor).
     * Usa credencial compartida con agenda-service
     * ({@code patiperro.agenda.interno.secret}).
     */
    public void marcarBloqueDisponibleInterno(Integer idAgendaBloque) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(agendaInternoSecret)) {
            throw new IllegalStateException(
                    "Falta patiperro.reserva.agenda-interno.secret para sincronizar cancelación con agenda-service");
        }
        try {
            restClient.patch()
                    .uri("/api/agenda/interno/bloques/{id}/marcar-disponible", idAgendaBloque)
                    .header(HEADER_AGENDA_INTERNO, agendaInternoSecret)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service (interno) respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    /**
     * Marca bloque reservado tras cobro confirmado (sin JWT del tutor). Idempotente en agenda si ya estaba reservado.
     */
    public void marcarBloqueReservadoInterno(Integer idAgendaBloque) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(agendaInternoSecret)) {
            throw new IllegalStateException(
                    "Falta patiperro.reserva.agenda-interno.secret para marcar reservado interno en agenda-service");
        }
        try {
            restClient.patch()
                    .uri("/api/agenda/interno/bloques/{id}/marcar-reservado", idAgendaBloque)
                    .header(HEADER_AGENDA_INTERNO, agendaInternoSecret)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service (interno marcar-reservado) respondió " + e.getStatusCode() + ": "
                            + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    public AgendaBloqueResumenDTO obtenerBloque(Integer idAgendaBloque, String rawJwt) {
        if (!isEnabled()) {
            return null;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para consultar agenda-service");
        }
        try {
            return restClient.get()
                    .uri("/api/agenda/bloques/{id}", idAgendaBloque)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .body(AgendaBloqueResumenDTO.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service respondio " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }

    private void patchBloque(Integer idAgendaBloque, String rawJwt, String uriTemplate) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para sincronizar con agenda-service");
        }
        try {
            restClient.patch()
                    .uri(uriTemplate, idAgendaBloque)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Agenda-service respondio " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar agenda-service: " + e.getMessage(), e);
        }
    }
}
