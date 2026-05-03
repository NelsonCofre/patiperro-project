package com.patiperro.pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subconjunto del JSON de {@code POST /checkout/preferences} (Checkout Pro).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPreferenceResponseDto(
        @JsonProperty("id") String id,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("sandbox_init_point") String sandboxInitPoint
) {
}
