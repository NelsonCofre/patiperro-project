package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuración de retiros del paseador.
 */
@ConfigurationProperties(prefix = "patiperro.pagos.retiro")
public class RetiroProperties {

    /**
     * Monto mínimo permitido para solicitar un retiro (en moneda local, típicamente CLP).
     */
    private BigDecimal minimo = new BigDecimal("1000.00");

    public BigDecimal getMinimo() {
        return minimo;
    }

    public void setMinimo(BigDecimal minimo) {
        this.minimo = minimo;
    }
}

