package com.patiperro.paseador.client;

import com.patiperro.paseador.dto.agenda.AgendaBloqueOfertaJsonDTO;
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
 * (mismo identificador que {@code id_paseador} en este dominio). Soporta búsqueda por franja
 * horaria o desde una fecha (típicamente hoy en agenda-service).
 * La exclusión por bloqueo personal de día completo la aplica agenda-service en ambas APIs.
 * {@link #listarBloquesOfertables(int, LocalDate, LocalDate)} consulta la oferta por rango de fechas.
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

    /**
     * Paseadores con al menos un bloque disponible desde hoy (fecha del servidor en agenda-service).
     *
     * @param idEstadoDisponible id del estado "disponible" en catálogo de agenda
     */
    public List<Integer> idsConBloqueDisponibleDesdeHoy(int idEstadoDisponible) {
        return idsConBloqueDisponibleDesdeFecha(null, idEstadoDisponible);
    }

    /**
     * Paseadores con al menos un bloque disponible desde {@code desdeFecha} inclusive.
     * Si {@code desdeFecha} es {@code null}, agenda-service usa la fecha actual.
     *
     * @param desdeFecha         opcional (ISO fecha); {@code null} = hoy en el servidor de agenda
     * @param idEstadoDisponible id del estado "disponible" en catálogo de agenda
     */
    public List<Integer> idsConBloqueDisponibleDesdeFecha(LocalDate desdeFecha, int idEstadoDisponible) {
        try {
            List<Integer> body = restClient
                    .get()
                    .uri(uriBuilder -> {
                        var ub = uriBuilder
                                .path("/api/agenda/bloques/busqueda/disponibles-desde-hoy")
                                .queryParam("idEstadoDisponible", idEstadoDisponible);
                        if (desdeFecha != null) {
                            ub.queryParam("desdeFecha", desdeFecha.toString());
                        }
                        return ub.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Integer>>() {});
            return body != null ? body : List.of();
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "No se pudo consultar disponibilidad desde-hoy en agenda-service: " + e.getMessage(), e);
        }
    }

    /**
     * Bloques en rango (oferta para tutores), vía agenda-service.
     * Excluye fechas con bloqueo personal de día completo.
     * <p><b>Advertencia (identificadores):</b> se asume que el id enviado a agenda es el mismo que
     * {@code id_paseador} en paseadores-service. Si el modelo separa usuario de paseador, la consulta
     * podría ir al usuario equivocado o devolver vacío sin un fallo obvio; usar entonces un mapeo
     * explícito paseador → usuario de agenda.
     *
     * @param idUsuario mismo identificador que {@code id_paseador} en paseadores-service (mientras dure esa convención)
     */
    public List<AgendaBloqueOfertaJsonDTO> listarBloquesOfertables(int idUsuario, LocalDate desde, LocalDate hasta) {
        try {
            List<AgendaBloqueOfertaJsonDTO> body = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/agenda/bloques/usuario/{idUsuario}/oferta")
                            .queryParam("desde", desde.toString())
                            .queryParam("hasta", hasta.toString())
                            .build(idUsuario))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AgendaBloqueOfertaJsonDTO>>() {});
            return body != null ? body : List.of();
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "No se pudo consultar oferta de agenda-service: " + e.getMessage(), e);
        }
    }
}
