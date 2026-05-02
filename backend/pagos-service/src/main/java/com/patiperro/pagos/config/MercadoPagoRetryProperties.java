package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reintentos ante fallos transitorios (429, 5xx, red) en llamadas a la API de Mercado Pago.
 * <p>Propiedades: {@code patiperro.mercadopago.retry.*} en {@code application.properties}.</p>
 */
@ConfigurationProperties(prefix = "patiperro.mercadopago.retry")
public class MercadoPagoRetryProperties {

    /**
     * Intentos por operación (GET payment, POST refund). Mínimo efectivo 1.
     */
    private int maxAttempts = 3;

    /**
     * Espera inicial entre reintentos (backoff exponencial), en ms.
     */
    private long initialDelayMs = 250;

    /**
     * Tope del backoff entre reintentos, en ms.
     */
    private long maxDelayMs = 4000;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }
}
