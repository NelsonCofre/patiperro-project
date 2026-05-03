package com.patiperro.reserva.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta alineada con pagos-service {@code PreferenciaCheckoutResponse} (Checkout Pro).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TutorCheckoutPreferenciaResponseDTO(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint,
        String urlCheckout
) {
}
