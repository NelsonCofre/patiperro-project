package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.MascotaResumenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MascotaIntegracionClient {

    private final RestClient restClient;

    public MascotaIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.mascotas.base-url:http://localhost:8083}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public MascotaResumenDTO obtenerResumen(Integer idMascota, String rawJwt) {
        if (idMascota == null) {
            return null;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para consultar mascotas-service");
        }
        try {
            return restClient.get()
                    .uri("/api/mascotas/{id}", idMascota)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .body(MascotaResumenDTO.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Mascotas-service respondio " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar mascotas-service: " + e.getMessage(), e);
        }
    }
}
