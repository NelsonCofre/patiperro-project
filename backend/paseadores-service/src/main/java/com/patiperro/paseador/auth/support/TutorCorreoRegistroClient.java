package com.patiperro.paseador.auth.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TutorCorreoRegistroClient {

    private static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final RestClient restClient;
    private final String internoSecret;

    public TutorCorreoRegistroClient(
            @Value("${patiperro.paseadores.integracion.tutores.base-url:http://localhost:8081}") String baseUrl,
            @Value("${patiperro.paseadores.integracion.tutores.interno.secret:}") String internoSecret) {
        this.restClient = RestClient.builder().baseUrl(baseUrl.replaceAll("/$", "")).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isConfigured() {
        return StringUtils.hasText(internoSecret);
    }

    public boolean existeCorreo(String correoNormalizado) {
        if (!StringUtils.hasText(correoNormalizado) || !isConfigured()) {
            return false;
        }
        try {
            CorreoExisteResponse body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/tutores/interno/correo/existe")
                            .queryParam("correo", correoNormalizado)
                            .build())
                    .header(HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(CorreoExisteResponse.class);
            return body != null && body.existe();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("No se pudo validar si el correo ya está en uso. Intenta más tarde.");
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo validar si el correo ya está en uso. Intenta más tarde.");
        }
    }

    private record CorreoExisteResponse(boolean existe) {
    }
}
