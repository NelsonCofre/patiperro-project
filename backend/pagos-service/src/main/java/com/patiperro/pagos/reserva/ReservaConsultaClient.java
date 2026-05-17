package com.patiperro.pagos.reserva;

import com.patiperro.pagos.reserva.dto.ReservaBilleteraDetalleDto;
import com.patiperro.pagos.reserva.dto.ReservaConsultaDto;
import com.patiperro.pagos.reserva.dto.ReservaInternoBilleteraDetallesRequest;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReservaConsultaClient {

    /** Mensaje de {@link IllegalArgumentException} si GET interno comprobante responde 404 (para mapeo HTTP en callers). */
    public static final String MSG_RESERVA_NO_ENCONTRADA_COMPROBANTE = "Reserva no encontrada";

    /** {@link IllegalArgumentException} cuando falta id reserva antes del GET interno. */
    public static final String MSG_ID_RESERVA_OBLIGATORIO = "idReserva es obligatorio";

    /** {@link IllegalStateException} cuando falta base-url o secreto para llamar a reserva-service. */
    public static final String MSG_INTEGRACION_RESERVA_NO_CONFIGURADA =
            "Integración reserva no configurada (base-url o secreto interno)";

    public static final String MSG_CONFLICTO_COMPROBANTE_INTERNO = "Conflicto al obtener comprobante interno";

    public static final String MSG_PREFIJO_RESERVA_HTTP_ERROR = "reserva-service respondió ";

    public static final String MSG_RESERVA_RED_NO_DISPONIBLE = "No se pudo consultar reserva-service";

    private static final Logger log = LoggerFactory.getLogger(ReservaConsultaClient.class);

    private final RestClient restClient;
    private final String internoSecret;
    private final boolean billeteraDetallesEnabled;

    public ReservaConsultaClient(
            RestClient.Builder restClientBuilder,
            @Value("${patiperro.pagos.integracion.reserva.base-url:http://localhost:8090}") String baseUrl,
            @Value("${patiperro.pagos.integracion.reserva.interno.secret:}") String internoSecret,
            @Value("${patiperro.pagos.integracion.reserva.billetera-detalles.enabled:true}")
                    boolean billeteraDetallesEnabled) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = base.isEmpty() ? null : restClientBuilder.baseUrl(base).build();
        this.internoSecret = internoSecret != null ? internoSecret.trim() : "";
        this.billeteraDetallesEnabled = billeteraDetallesEnabled;
    }

    private boolean isBilleteraDetallesConfigured() {
        return billeteraDetallesEnabled && restClient != null && StringUtils.hasText(internoSecret);
    }

    /**
     * Enriquecimiento opcional para GET billetera paseador. Si falla la red o está deshabilitado,
     * devuelve mapa vacío (la billetera sigue respondiendo con montos y fases).
     */
    public Map<Integer, ReservaBilleteraDetalleDto> obtenerDetallesBilleteraPaseador(
            Long idUsuarioPaseador, List<Integer> idsReserva) {
        if (!isBilleteraDetallesConfigured()
                || idUsuarioPaseador == null
                || idsReserva == null
                || idsReserva.isEmpty()) {
            return Map.of();
        }
        try {
            List<ReservaBilleteraDetalleDto> lista = restClient
                    .post()
                    .uri("/api/reserva/interno/billetera/detalles-paseador")
                    .header(ReservaPagosIntegracionClient.HEADER_INTERNO, internoSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservaInternoBilleteraDetallesRequest(idUsuarioPaseador, idsReserva))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ReservaBilleteraDetalleDto>>() {});
            if (lista == null || lista.isEmpty()) {
                return Map.of();
            }
            Map<Integer, ReservaBilleteraDetalleDto> map = new LinkedHashMap<>();
            for (ReservaBilleteraDetalleDto d : lista) {
                if (d != null && d.idReserva() != null) {
                    map.put(d.idReserva(), d);
                }
            }
            return map;
        } catch (RestClientResponseException e) {
            log.warn("reserva-service billetera-detalles HTTP {} — ítems sin enriquecer", e.getStatusCode());
            return Map.of();
        } catch (RestClientException e) {
            log.warn("reserva-service billetera-detalles no disponible — ítems sin enriquecer", e);
            return Map.of();
        }
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

    public ReservaComprobanteDto obtenerComprobanteInterno(Long idReserva) {
        if (idReserva == null) {
            throw new IllegalArgumentException(MSG_ID_RESERVA_OBLIGATORIO);
        }
        if (restClient == null || !StringUtils.hasText(internoSecret)) {
            throw new IllegalStateException(MSG_INTEGRACION_RESERVA_NO_CONFIGURADA);
        }
        try {
            return restClient.get()
                    .uri("/api/reserva/interno/{id}/comprobante", idReserva)
                    .header(ReservaPagosIntegracionClient.HEADER_INTERNO, internoSecret)
                    .retrieve()
                    .body(ReservaComprobanteDto.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException(MSG_RESERVA_NO_ENCONTRADA_COMPROBANTE);
            }
            if (e.getStatusCode().value() == 409) {
                throw new IllegalStateException(MSG_CONFLICTO_COMPROBANTE_INTERNO);
            }
            throw new IllegalStateException(MSG_PREFIJO_RESERVA_HTTP_ERROR + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException(MSG_RESERVA_RED_NO_DISPONIBLE, e);
        }
    }
}
