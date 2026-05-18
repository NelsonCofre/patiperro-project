package com.patiperro.pagos.checkout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload del Payment Brick (Checkout API): token y datos del medio de pago generados en el navegador.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrickPagoRequest(
        Long idReserva,
        String token,
        String paymentMethodId,
        Integer installments,
        String issuerId,
        String payerEmail,
        String identificationType,
        String identificationNumber
) {
}
