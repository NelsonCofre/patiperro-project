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
 * Consulta agenda-service: paseadores con bloque disponible en una franja (mismo {@code id_usuario} que
 * {@code id_paseador} en este dominio).
 */
@Component
public class AgendaDisponibilidadClient {

    private final RestClient restClient;

    public AgendaDisponibilidadClient(@Value("${patiperro.agenda.base-url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl.replaceAll("/$", "")).build();
    }

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
