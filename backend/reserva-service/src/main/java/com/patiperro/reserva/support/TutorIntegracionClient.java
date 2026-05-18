package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.integracion.TutorCorreoInternoDTO;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TutorIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(TutorIntegracionClient.class);

    private static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final String tutoresInternoSecret;

    public TutorIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.tutores.base-url:http://localhost:8081}") String baseUrl,
            @Value("${patiperro.reserva.tutores-interno.secret:}") String tutoresInternoSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.tutoresInternoSecret = tutoresInternoSecret != null ? tutoresInternoSecret.trim() : "";
    }

    /**
     * Correo del tutor vía endpoint interno de tutores-service (sin JWT).
     */
    public String obtenerCorreoInterno(Long idTutor) {
        if (idTutor == null || !StringUtils.hasText(tutoresInternoSecret)) {
            return null;
        }
        try {
            TutorCorreoInternoDTO dto = restClient.get()
                    .uri("/api/tutores/interno/{id}/correo", idTutor)
                    .header(HEADER_INTERNO, tutoresInternoSecret)
                    .retrieve()
                    .body(TutorCorreoInternoDTO.class);
            return dto != null && StringUtils.hasText(dto.correo()) ? dto.correo().trim() : null;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            log.warn("tutores interno: status {} al obtener correo tutor {}", e.getStatusCode(), idTutor);
            return null;
        } catch (RestClientException e) {
            log.warn("tutores interno: sin respuesta al obtener correo tutor {}", idTutor, e);
            return null;
        }
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
