package com.patiperro.reserva.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ajustes de billetera en pagos-service (retenido / verificación / liberación N+2).
 * Reutiliza la misma configuración base que {@link PagosReembolsoIntegracionClient}.
 */
@Component
public class PagosBilleteraIntegracionClient {

    private static final Logger log = LoggerFactory.getLogger(PagosBilleteraIntegracionClient.class);

    private static final String HEADER_INTERNO = PagosReembolsoIntegracionClient.HEADER_INTERNO;

    private static final String URI_ACREDITAR = "/api/pagos/interno/billetera/acreditar-retenido";
    private static final String URI_VERIFICACION = "/api/pagos/interno/billetera/pasar-verificacion";
    private static final String URI_REVERTIR = "/api/pagos/interno/billetera/revertir-retenido";

    private final RestClient restClient;
    private final boolean enabled;
    private final String internoSecret;

    public PagosBilleteraIntegracionClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.enabled:false}") boolean enabled,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.base-url:http://localhost:8087}") String baseUrl,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.interno.secret:}") String internoSecret,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${patiperro.reserva.integracion.pagos-reembolso.read-timeout-ms:30000}") long readTimeoutMs) {
        this.enabled = enabled;
        String base = normalizeBaseUrl(baseUrl);
        this.restClient = base.isEmpty()
                ? null
                : restClientBuilder
                        .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                        .baseUrl(base)
                        .build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
    }

    public boolean isEnabled() {
        return enabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    public void acreditarRetenido(Integer idReserva, long idUsuarioPaseador, long idTransaccionPagos) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("idUsuarioPaseador", idUsuarioPaseador);
        body.put("idTransaccionPagos", idTransaccionPagos);
        postSilencioso(URI_ACREDITAR, body, "acreditar-retenido", idReserva);
    }

    public void pasarAVerificacion(Integer idReserva, long idUsuarioPaseador, LocalDateTime fechaFinServicio) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idReserva", idReserva);
        body.put("idUsuarioPaseador", idUsuarioPaseador);
        if (fechaFinServicio != null) {
            body.put("fechaFinServicio", fechaFinServicio);
        }
        postSilencioso(URI_VERIFICACION, body, "pasar-verificacion", idReserva);
    }

    public void revertirRetenido(Integer idReserva) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> body = Map.of("idReserva", idReserva);
        postSilencioso(URI_REVERTIR, body, "revertir-retenido", idReserva);
    }

    private void postSilencioso(String uri, Map<String, Object> body, String op, Integer idReserva) {
        try {
            restClient.post()
                    .uri(uri)
                    .header(HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Void.class);
        } catch (RestClientResponseException e) {
            log.warn("Pagos billetera {} falló HTTP {} (idReserva={})", op, e.getStatusCode().value(), idReserva);
        } catch (RestClientException e) {
            log.warn("Pagos billetera {} error de red (idReserva={})", op, idReserva, e);
        }
    }

    private static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String b = raw.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static SimpleClientHttpRequestFactory requestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(connectTimeoutMs, 120_000L))));
        factory.setReadTimeout(Duration.ofMillis(Math.max(1_000L, Math.min(readTimeoutMs, 600_000L))));
        return factory;
    }
}
