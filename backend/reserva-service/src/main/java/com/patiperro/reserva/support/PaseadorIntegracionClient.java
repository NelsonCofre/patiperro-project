package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.PaseadorResumenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PaseadorIntegracionClient {

    private final RestClient restClient;

    public PaseadorIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.paseadores.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public PaseadorResumenDTO obtenerResumen(Integer idPaseador) {
        if (idPaseador == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/paseadores/public/{idPaseador}/resumen", idPaseador)
                    .retrieve()
                    .body(PaseadorResumenDTO.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Paseadores-service respondio " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar paseadores-service: " + e.getMessage(), e);
        }
    }
}
