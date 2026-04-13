package com.patiperro.paseador.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cliente HTTP hacia agenda-service: lista de {@code id_usuario} con bloque disponible
 * en la franja pedida (mismo identificador que {@code id_paseador} en este dominio).
 * La exclusión por bloqueo personal de día completo la aplica agenda-service en esa API.
 */
@Component
public class AgendaDisponibilidadClient {

    private final RestClient restClient;

    public AgendaDisponibilidadClient(@Value("${patiperro.agenda.base-url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl.replaceAll("/$", "")).build();
    }

    /**
     * @param fecha            día calendario de la búsqueda (debe coincidir con la parte fecha de horaInicio/horaFin)
     * @param horaInicio       inicio de la franja (ISO-8601 local date-time)
     * @param horaFin          fin de la franja (ISO-8601 local date-time)
     * @param idEstadoDisponible id del estado "disponible" en catálogo de agenda
     * @return ids de usuario elegibles; vacío si ninguno cumple (incl. si el día está bloqueado personalmente)
     */
    public List<Integer> idsConBloqueDisponible(
            LocalDate fecha,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            int idEstadoDisponible) {
        DateTimeFormatter dt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            List<Integer> body = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/agenda/bloques/busqueda/disponibles")
                            .queryParam("fecha", fecha.toString())
                            .queryParam("horaInicio", horaInicio.format(dt))
                            .queryParam("horaFin", horaFin.format(dt))
                            .queryParam("idEstadoDisponible", idEstadoDisponible)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Integer>>() {});
            return body != null ? body : List.of();
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "No se pudo consultar disponibilidad en agenda-service: " + e.getMessage(), e);
        }
    }
}
