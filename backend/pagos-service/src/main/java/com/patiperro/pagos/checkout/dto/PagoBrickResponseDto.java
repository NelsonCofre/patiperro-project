package com.patiperro.pagos.checkout.dto;

/**
 * Respuesta tras procesar un pago con token (sin preferencia Checkout Pro).
 */
public record PagoBrickResponseDto(
        String mpPaymentId,
        String mpStatus,
        String mpStatusDetail
) {
}
