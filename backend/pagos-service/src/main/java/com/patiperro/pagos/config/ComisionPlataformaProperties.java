package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Comisión de la plataforma sobre el monto bruto del cobro (reserva).
 * <p>Configurable vía {@code patiperro.pagos.comision.plataforma-tasa} (fracción 0–1, ej. {@code 0.05} = 5%).</p>
 */
@ConfigurationProperties(prefix = "patiperro.pagos.comision")
public class ComisionPlataformaProperties {

    /**
     * Fracción aplicada al monto bruto (tutor/pasarela). Por defecto alineado al front histórico (~5%).
     */
    private BigDecimal plataformaTasa = new BigDecimal("0.05");

    public BigDecimal getPlataformaTasa() {
        return plataformaTasa;
    }

    public void setPlataformaTasa(BigDecimal plataformaTasa) {
        this.plataformaTasa = plataformaTasa;
    }
}
