package com.patiperro.pagos.support;

/**
 * Error al crear un pago {@code POST /v1/payments} en Mercado Pago (sin preferencia / Checkout API).
 */
public final class MercadoPagoCrearPagoException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;

    public MercadoPagoCrearPagoException(int httpStatus, String responseBody) {
        super("Mercado Pago crear pago HTTP " + httpStatus);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody == null ? "" : responseBody;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
