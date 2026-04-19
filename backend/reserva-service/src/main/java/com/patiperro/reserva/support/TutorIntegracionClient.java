package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TutorIntegracionClient {

    private final RestClient restClient;

    public TutorIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.tutores.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Perfil del tutor. Cualquier JWT firmado con la misma clave (p. ej. paseador) pasa el filtro
     * de tutores-service; se usa el token del usuario autenticado en el gateway.
     */
    public TutorReservaClientDTO obtenerTutor(Long idTutor, String rawJwt) {
        if (idTutor == null) {
            return null;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT para consultar tutores-service");
        }
        try {
            return restClient.get()
                    .uri("/api/tutores/{id}", idTutor)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawJwt.trim())
                    .retrieve()
                    .body(TutorReservaClientDTO.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw new IllegalStateException(
                    "Tutores-service respondio " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar tutores-service: " + e.getMessage(), e);
        }
    }
}
