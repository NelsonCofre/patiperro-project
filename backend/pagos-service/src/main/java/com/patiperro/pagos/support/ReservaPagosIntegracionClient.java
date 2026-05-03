package com.patiperro.pagos.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Llamadas internas a {@code reserva-service} (vínculo transacción / pago aprobado).
 */
@Component
public class ReservaPagosIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(ReservaPagosIntegracionClient.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private static final String URI_PAGO_APROBADO = "/api/reserva/interno/pagos/mercadopago/pago-aprobado";
    private static final String URI_VINCULO_TX = "/api/reserva/interno/pagos/mercadopago/vinculo-transaccion";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public ReservaPagosIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.reserva.enabled:true}") boolean enabled,
            @Value("${patiperro.pagos.integracion.reserva.base-url:http://localhost:8085}") String baseUrl,
            @Value("${patiperro.pagos.integracion.reserva.interno.secret:}") String internoSecret) {
        this.enabled = enabled;
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    /**
     * Persiste en reserva el id de la transacción en pagos (checkout iniciado).
     */
    public void vincularTransaccionReserva(Integer idReserva, Long idTransaccionPagos) {
        if (idReserva == null || idTransaccionPagos == null) {
            log.warn("Integración reserva: vincular transacción omitido (idReserva o idTransaccion nulo)");
            return;
        }
        if (!enabled || restClient == null) {
            log.debug("Integración reserva deshabilitada o sin base-url; omitido vincular (reserva={})", idReserva);
            return;
        }
        if (!StringUtils.hasText(internoSecret)) {
            log.warn("Integración reserva: falta secreto interno; omitido vincular (reserva={})", idReserva);
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("idReserva", idReserva);
            body.put("idTransaccionPagos", idTransaccionPagos);
            restClient.post()
                    .uri(URI_VINCULO_TX)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Integración reserva: vincular transacción no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Integración reserva: vincular transacción no completada (reserva={})", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Integración reserva: vincular transacción error inesperado (reserva={})", idReserva, e);
        }
    }

    /**
     * Marca la reserva pagada; {@code idTransaccionPagos} es {@code transaccion.id_transaccion} en esta BD.
     */
    public void notificarPagoAprobado(Integer idReserva, Long idTransaccionPagos, String mpPaymentIdOpcional) {
        if (idReserva == null || idTransaccionPagos == null) {
            log.warn("Integración reserva: idReserva o idTransaccionPagos vacío; no se invoca pago-aprobado");
            return;
        }
        if (!enabled || restClient == null) {
            log.debug("Integración reserva deshabilitada o sin base-url; omitido (reserva={})", idReserva);
            return;
        }
        if (!StringUtils.hasText(internoSecret)) {
            log.warn("Integración reserva: falta patiperro.pagos.integracion.reserva.interno.secret; omitido (reserva={})",
                    idReserva);
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("idReserva", idReserva);
            body.put("idTransaccionPagos", idTransaccionPagos);
            if (StringUtils.hasText(mpPaymentIdOpcional)) {
                body.put("mpPaymentId", mpPaymentIdOpcional.trim());
            }
            restClient.post()
                    .uri(URI_PAGO_APROBADO)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Integración reserva: respuesta no exitosa (reserva={}, status={})", idReserva,
                    e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("Integración reserva: llamada no completada para reserva {}", idReserva, e);
        } catch (RuntimeException e) {
            log.warn("Integración reserva: error inesperado para reserva {}", idReserva, e);
        }
    }
}
