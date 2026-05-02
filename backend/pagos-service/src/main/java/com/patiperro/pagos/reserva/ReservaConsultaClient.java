package com.patiperro.pagos.reserva;

import com.patiperro.pagos.reserva.dto.ReservaConsultaDto;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ReservaConsultaClient {

    private final RestClient restClient;
    private final String internoSecret;

    public ReservaConsultaClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.reserva.base-url:http://localhost:8085}") String baseUrl,
            @Value("${patiperro.pagos.integracion.reserva.interno.secret:}") String internoSecret) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public ReservaConsultaDto obtenerReservaParaPago(Long idReserva) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }
        if (restClient == null || !StringUtils.hasText(internoSecret)) {
            throw new IllegalStateException("Integración reserva no configurada (base-url o secreto interno)");
        }
        try {
            return restClient.get()
                    .uri("/api/reserva/interno/{id}/para-pago", idReserva)
                    .header(ReservaPagosIntegracionClient.HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(ReservaConsultaDto.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("Reserva no encontrada");
            }
            if (e.getStatusCode().value() == 409) {
                throw new IllegalStateException("Estado de reserva no permite iniciar pago");
            }
            throw new IllegalStateException("reserva-service respondió " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo consultar reserva-service", e);
        }
    }
}
