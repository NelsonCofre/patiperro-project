package com.patiperro.reserva.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "patiperro.reserva.integracion.pagos-billetera")
public class PagosBilleteraIntegracionProperties {

    private String enabled = "";
    private String baseUrl = "";

    @NestedConfigurationProperty
    private final Interno interno = new Interno();

    private String connectTimeoutMs = "";
    private String readTimeoutMs = "";

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Interno getInterno() {
        return interno;
    }

    public String getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(String connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public String getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(String readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public static class Interno {

        private String secret = "";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
