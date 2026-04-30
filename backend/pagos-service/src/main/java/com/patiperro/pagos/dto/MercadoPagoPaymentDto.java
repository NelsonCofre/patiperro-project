package com.patiperro.pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subconjunto del JSON de {@code GET /v1/payments/{id}} de Mercado Pago.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPaymentDto(
        @JsonProperty("id") Long id,
        @JsonProperty("status") String status,
        @JsonProperty("external_reference") String externalReference
) {
    public String idAsString() {
        return id == null ? null : String.valueOf(id);
    }
}
