package com.patiperro.pagos.dto.checkout;

/**
 * Respuesta al crear una preferencia: enlaces MP brutos y {@code urlCheckout} según {@code patiperro.mercadopago.checkout.use-sandbox}.
 */
public record PreferenciaCheckoutResponse(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint,
        String urlCheckout
) {
}
