package com.patiperro.pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Subconjunto del JSON de {@code GET /v1/payments/{id}} de Mercado Pago.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPaymentDto(
        @JsonProperty("id") Long id,
        @JsonProperty("status") String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("currency_id") String currencyId,
        @JsonProperty("date_approved") String dateApproved,
        @JsonProperty("payment_type_id") String paymentTypeId,
        @JsonProperty("refunds") List<Object> refunds
) {
    public MercadoPagoPaymentDto {
        refunds = refunds == null ? List.of() : refunds;
    }

    public String idAsString() {
        return id == null ? null : String.valueOf(id);
    }

    public boolean tieneReembolsosRegistrados() {
        return refunds != null && !refunds.isEmpty();
    }
}
