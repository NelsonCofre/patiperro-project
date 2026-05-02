package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de retorno Checkout Pro y opciones asociadas.
 * <p>Propiedades: {@code patiperro.mercadopago.checkout.*}</p>
 */
@ConfigurationProperties(prefix = "patiperro.mercadopago.checkout")
public class MercadoPagoCheckoutProperties {

    /**
     * URL del front tras pago aprobado ({@code back_urls.success}).
     */
    private String backUrlSuccess = "";

    private String backUrlFailure = "";

    private String backUrlPending = "";

    /**
     * Valor MP para {@code auto_return} (p. ej. {@code approved}).
     */
    private String autoReturn = "approved";

    /**
     * Webhook/IPN URL opcional en la preferencia (si vacío, no se envía).
     */
    private String notificationUrl = "";

    /**
     * Si {@code true}, en la respuesta de checkout el campo {@code urlCheckout} usa la URL sandbox de MP.
     */
    private boolean useSandbox = false;

    public String getBackUrlSuccess() {
        return backUrlSuccess;
    }

    public void setBackUrlSuccess(String backUrlSuccess) {
        this.backUrlSuccess = backUrlSuccess == null ? "" : backUrlSuccess.trim();
    }

    public String getBackUrlFailure() {
        return backUrlFailure;
    }

    public void setBackUrlFailure(String backUrlFailure) {
        this.backUrlFailure = backUrlFailure == null ? "" : backUrlFailure.trim();
    }

    public String getBackUrlPending() {
        return backUrlPending;
    }

    public void setBackUrlPending(String backUrlPending) {
        this.backUrlPending = backUrlPending == null ? "" : backUrlPending.trim();
    }

    public String getAutoReturn() {
        return autoReturn;
    }

    public void setAutoReturn(String autoReturn) {
        this.autoReturn = (autoReturn == null || autoReturn.isBlank()) ? "approved" : autoReturn.trim();
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
    }

    /** {@code true} si las tres back URLs están definidas (requerido para Checkout Pro). */
    public boolean tieneBackUrlsCompletas() {
        return !backUrlSuccess.isEmpty() && !backUrlFailure.isEmpty() && !backUrlPending.isEmpty();
    }

    public boolean isUseSandbox() {
        return useSandbox;
    }

    public void setUseSandbox(boolean useSandbox) {
        this.useSandbox = useSandbox;
    }
}
