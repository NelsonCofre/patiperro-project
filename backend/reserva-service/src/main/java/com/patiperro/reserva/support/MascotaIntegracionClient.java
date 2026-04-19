package com.patiperro.reserva.support;

import com.patiperro.reserva.dto.MascotaPortadaUrlResponse;
import com.patiperro.reserva.dto.MascotaResumenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MascotaIntegracionClient {

    private static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final String internoSecret;

    public MascotaIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.mascotas.base-url:http://localhost:8083}") String baseUrl,
            @Value("${patiperro.reserva.mascotas-interno.secret:}") String internoSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
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

    /**
     * Portada de la mascota vía ruta interna (secreto compartido). Sin JWT de tutor.
     * Si el secreto no está configurado o no hay URL, devuelve {@code null}.
     */
    public MascotaPortadaUrlResponse obtenerPortadaInterno(Integer idMascota) {
        if (idMascota == null || !StringUtils.hasText(internoSecret)) {
            return null;
        }
        try {
            MascotaPortadaUrlResponse body = restClient.get()
                    .uri("/api/mascotas/interno/{id}/portada-url", idMascota.longValue())
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(MascotaPortadaUrlResponse.class);
            if (body == null || !StringUtils.hasText(body.getUrl())) {
                return null;
            }
            body.setUrl(body.getUrl().trim());
            if (body.getNombre() != null) {
                body.setNombre(body.getNombre().trim());
            }
            return body;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw new IllegalStateException(
                    "Mascotas-service (interno) respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo contactar mascotas-service: " + e.getMessage(), e);
        }
    }
}
