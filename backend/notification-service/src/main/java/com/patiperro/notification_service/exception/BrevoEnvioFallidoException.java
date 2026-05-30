package com.patiperro.notification_service.exception;

/**
 * Error al enviar una plantilla transactional por API Brevo ({@code procesarEventoUniversal}).
 */
public class BrevoEnvioFallidoException extends RuntimeException {

    private final String tipoEvento;

    public BrevoEnvioFallidoException(String tipoEvento, Throwable cause) {
        super("Fallo envío Brevo para evento " + tipoEvento, cause);
        this.tipoEvento = tipoEvento != null ? tipoEvento : "";
    }

    public String getTipoEvento() {
        return tipoEvento;
    }
}
