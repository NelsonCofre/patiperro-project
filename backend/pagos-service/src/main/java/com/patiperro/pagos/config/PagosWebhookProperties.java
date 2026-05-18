package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Comportamiento de auditoría / advertencias en el procesamiento del webhook de Mercado Pago (sin fallar el flujo).
 */
@ConfigurationProperties(prefix = "patiperro.pagos.webhook")
public class PagosWebhookProperties {

    /**
     * Loguear detalle del payload aprobado (moneda, fecha aprobación, tipo de pago).
     */
    private boolean logApprovedDetails = true;

    /**
     * Advertir si el monto del checkout (transacción pendiente) difiere del {@code transaction_amount} de MP.
     */
    private boolean warnOnCheckoutMpAmountMismatch = true;

    /**
     * Tolerancia en pesos CLP; por encima se emite {@code WARN} (no se aborta el proceso).
     */
    private BigDecimal amountMismatchToleranceClp = BigDecimal.ONE;

    /**
     * Advertir si {@code currency_id} del pago no coincide con {@link #expectedCurrencyId}.
     */
    private boolean warnOnCurrencyMismatch = true;

    /**
     * Valor esperado de {@code currency_id} para reservas Patiperro (típicamente CLP).
     */
    private String expectedCurrencyId = "CLP";

    public boolean isLogApprovedDetails() {
        return logApprovedDetails;
    }

    public void setLogApprovedDetails(boolean logApprovedDetails) {
        this.logApprovedDetails = logApprovedDetails;
    }

    public boolean isWarnOnCheckoutMpAmountMismatch() {
        return warnOnCheckoutMpAmountMismatch;
    }

    public void setWarnOnCheckoutMpAmountMismatch(boolean warnOnCheckoutMpAmountMismatch) {
        this.warnOnCheckoutMpAmountMismatch = warnOnCheckoutMpAmountMismatch;
    }

    public BigDecimal getAmountMismatchToleranceClp() {
        return amountMismatchToleranceClp;
    }

    public void setAmountMismatchToleranceClp(BigDecimal amountMismatchToleranceClp) {
        this.amountMismatchToleranceClp = amountMismatchToleranceClp;
    }

    public boolean isWarnOnCurrencyMismatch() {
        return warnOnCurrencyMismatch;
    }

    public void setWarnOnCurrencyMismatch(boolean warnOnCurrencyMismatch) {
        this.warnOnCurrencyMismatch = warnOnCurrencyMismatch;
    }

    public String getExpectedCurrencyId() {
        return expectedCurrencyId;
    }

    public void setExpectedCurrencyId(String expectedCurrencyId) {
        this.expectedCurrencyId = expectedCurrencyId;
    }
}
