package com.patiperro.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web Push del chat (VAPID).
 * <p>Propiedades: {@code patiperro.notification.webpush.*} en application-dev / application-prod.</p>
 * <p>La clave privada solo por entorno ({@code VAPID_PRIVATE_KEY}); no commitear en el repo.</p>
 */
@ConfigurationProperties(prefix = "patiperro.notification.webpush")
public class WebPushProperties {

    private static final int MIN_PAYLOAD_PREVIEW_CHARS = 20;
    private static final int MAX_PAYLOAD_PREVIEW_CHARS = 500;

    /**
     * Si {@code false}, no se envían push aunque existan suscripciones (arranque seguro sin VAPID).
     */
    private boolean enabled;

    private Vapid vapid = new Vapid();

    /**
     * Longitud máxima del texto del cuerpo en el payload JSON (FCM/Mozilla limitan tamaño total).
     */
    private int payloadPreviewChars = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Vapid getVapid() {
        return vapid;
    }

    public void setVapid(Vapid vapid) {
        this.vapid = vapid != null ? vapid : new Vapid();
    }

    public int getPayloadPreviewChars() {
        return normalizePreviewChars(payloadPreviewChars);
    }

    public void setPayloadPreviewChars(int payloadPreviewChars) {
        this.payloadPreviewChars = normalizePreviewChars(payloadPreviewChars);
    }

    /** {@code true} si el envío push está habilitado y VAPID está completo. */
    public boolean isReadyForSend() {
        if (!enabled) {
            return false;
        }
        return vapid.hasKeysForSend();
    }

    /** Clave pública disponible (p. ej. {@code GET /api/notificaciones/push/vapid-public-key}). */
    public boolean hasPublicKey() {
        return vapid.hasPublicKey();
    }

    private static int normalizePreviewChars(int value) {
        if (value < MIN_PAYLOAD_PREVIEW_CHARS) {
            return MIN_PAYLOAD_PREVIEW_CHARS;
        }
        return Math.min(value, MAX_PAYLOAD_PREVIEW_CHARS);
    }

    public static class Vapid {

        private String publicKey = "";
        private String privateKey = "";

        /** Contacto del emisor VAPID (mailto: o URL HTTPS). */
        private String subject = "mailto:soporte@patiperro.cl";

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey != null ? publicKey.trim() : "";
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey != null ? privateKey.trim() : "";
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject != null ? subject.trim() : "";
        }

        boolean hasPublicKey() {
            return hasText(publicKey);
        }

        boolean hasKeysForSend() {
            return hasText(publicKey) && hasText(privateKey) && hasText(subject);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
