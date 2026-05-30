package com.patiperro.pagos.dto.checkout;

import jakarta.validation.constraints.NotBlank;

public record SincronizarPagoCheckoutRequest(@NotBlank String paymentId) {
}
