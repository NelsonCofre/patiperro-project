package com.patiperro.pagos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Comisión de la plataforma sobre el monto bruto del cobro (reserva).
 * <p>
 * Propiedad: {@code patiperro.pagos.comision.plataforma-tasa}. Override por entorno:
 * {@code PATI_COMISION_PLATAFORMA_TASA} (misma semántica: fracción entre 0 y 1).
 * </p>
 * <p>
 * Formato: usar siempre fracción decimal, no “porcentaje entero” ({@code 0.15} = 15%, {@code 0.20} = 20%).
 * El valor por defecto del campo debe mantenerse alineado con el default en
 * {@code application.properties} ({@code :0.05}) para evitar discrepancias si la propiedad no se enlaza.
 * </p>
 * <p>
 * Validación de rango en arranque no está habilitada aquí a propósito: una config inválida podría impedir
 * el despliegue; el cálculo acota la tasa en {@link com.patiperro.pagos.service.NetoTransaccionService}.
 * </p>
 */
@ConfigurationProperties(prefix = "patiperro.pagos.comision")
public class ComisionPlataformaProperties {

    /**
     * Fracción del monto bruto (pasarela) retenida como comisión; el paseador recibe bruto × (1 − tasa)
     * tras redondeo en {@link com.patiperro.pagos.service.NetoTransaccionService}.
     */
    private BigDecimal plataformaTasa = new BigDecimal("0.05");

    public BigDecimal getPlataformaTasa() {
        return plataformaTasa;
    }

    public void setPlataformaTasa(BigDecimal plataformaTasa) {
        this.plataformaTasa = plataformaTasa;
    }
}
