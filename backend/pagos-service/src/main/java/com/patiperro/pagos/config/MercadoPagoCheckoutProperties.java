package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * URLs de retorno Checkout Pro y opciones asociadas.
 * <p>Propiedades: {@code patiperro.mercadopago.checkout.*}</p>
 */
@ConfigurationProperties(prefix = "patiperro.mercadopago.checkout")
public class MercadoPagoCheckoutProperties {

    /**
     * Base pública del front (HTTPS en túnel). Si no está vacía, se derivan las tres
     * {@code back_urls} como {@code base + /tutor/reservas/pago/exito|error|pendiente}.
     * Si está vacía, se usan {@link #backUrlSuccess}, {@link #backUrlFailure},
     * {@link #backUrlPending}.
     */
    private String publicFrontBaseUrl = "";

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

    public String getPublicFrontBaseUrl() {
        return publicFrontBaseUrl;
    }

    public void setPublicFrontBaseUrl(String publicFrontBaseUrl) {
        this.publicFrontBaseUrl = publicFrontBaseUrl == null ? "" : publicFrontBaseUrl.trim();
    }

    /** {@code true} si hay base pública o las tres URLs explícitas. */
    public boolean tieneBackUrlsCompletas() {
        return !normalizarBase(publicFrontBaseUrl).isEmpty()
                || (!backUrlSuccess.isEmpty() && !backUrlFailure.isEmpty() && !backUrlPending.isEmpty());
    }

    public String resolveBackUrlSuccess() {
        String fromBase = urlDesdeBasePublica("/tutor/reservas/pago/exito");
        return fromBase != null ? fromBase : backUrlSuccess;
    }

    public String resolveBackUrlFailure() {
        String fromBase = urlDesdeBasePublica("/tutor/reservas/pago/error");
        return fromBase != null ? fromBase : backUrlFailure;
    }

    public String resolveBackUrlPending() {
        String fromBase = urlDesdeBasePublica("/tutor/reservas/pago/pendiente");
        return fromBase != null ? fromBase : backUrlPending;
    }

    private static String normalizarBase(String base) {
        if (base == null || base.isBlank()) {
            return "";
        }
        return base.replaceAll("/+$", "");
    }

    /**
     * Si {@link #publicFrontBaseUrl} está definida, devuelve base + path (path debe empezar
     * con {@code /}). Si no, {@code null} para usar la URL explícita correspondiente.
     */
    private String urlDesdeBasePublica(String pathRelativo) {
        String base = normalizarBase(publicFrontBaseUrl);
        if (base.isEmpty()) {
            return null;
        }
        String path = Objects.requireNonNull(pathRelativo);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    public boolean isUseSandbox() {
        return useSandbox;
    }

    public void setUseSandbox(boolean useSandbox) {
        this.useSandbox = useSandbox;
    }
}
