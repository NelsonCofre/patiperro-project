package com.patiperro.pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de {@code POST /v1/payments/{id}/refunds} en Mercado Pago.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoRefundResponseDto(
        @JsonProperty("id") Long id,
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("status") String status
) {
    public String idAsString() {
        return id == null ? null : String.valueOf(id);
    }
}
