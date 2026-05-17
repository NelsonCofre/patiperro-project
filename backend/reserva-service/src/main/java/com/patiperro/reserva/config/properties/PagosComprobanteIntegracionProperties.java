package com.patiperro.reserva.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "patiperro.reserva.integracion.pagos-comprobante")
public class PagosComprobanteIntegracionProperties {

    private boolean enabled = false;

    /** Vacío = heredar base-url de {@code pagos-reembolso}. */
    private String baseUrl = "";

    private long connectTimeoutMs = 5000;

    /** 0 = derivar de pagos-reembolso con tope 45s para hilos post-commit. */
    private long readTimeoutMs = 0;

    @NestedConfigurationProperty
    private final Interno interno = new Interno();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Interno getInterno() {
        return interno;
    }

    public static class Interno {

        /** Vacío = heredar secreto de pagos-reembolso. */
        private String secret = "";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
