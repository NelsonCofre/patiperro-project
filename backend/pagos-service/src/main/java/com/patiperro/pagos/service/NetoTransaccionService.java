package com.patiperro.pagos.service;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.Transaccion;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deducción de la comisión de la plataforma sobre el monto bruto y cálculo del neto para el paseador / dominio interno.
 */
@Service
@RequiredArgsConstructor
public class NetoTransaccionService {

    private static final Logger log = LoggerFactory.getLogger(NetoTransaccionService.class);

    private static final int SCALE_MONEDA = 2;

    private final ComisionPlataformaProperties comisionPlataformaProperties;

    /**
     * Monto bruto autoritativo: preferir {@code transaction_amount} del pago MP; si falta o es inválido, usar el bruto ya persistido en la transacción (checkout).
     */
    public BigDecimal resolverMontoBruto(MercadoPagoPaymentDto pago, Transaccion tx) {
        BigDecimal desdeMp = pago != null ? pago.transactionAmount() : null;
        if (desdeMp != null && desdeMp.signum() > 0) {
            return desdeMp.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        }
        if (tx != null && tx.getMontoBruto() != null && tx.getMontoBruto().signum() > 0) {
            return tx.getMontoBruto().setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
    }

    /**
     * Aplica la tasa configurada al bruto y devuelve comisión + neto (escala monetaria fija).
     */
    public ResultadoNeto calcular(BigDecimal montoBruto) {
        BigDecimal bruto = montoBruto != null ? montoBruto : BigDecimal.ZERO;
        bruto = bruto.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);

        if (bruto.signum() <= 0) {
            BigDecimal z = BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
            return new ResultadoNeto(z, z);
        }

        BigDecimal tasa = comisionPlataformaProperties.getPlataformaTasa();
        if (tasa == null) {
            tasa = BigDecimal.ZERO;
        }
        tasa = tasa.max(BigDecimal.ZERO).min(BigDecimal.ONE);

        BigDecimal comision = bruto.multiply(tasa).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        BigDecimal neto = bruto.subtract(comision).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        if (neto.signum() < 0) {
            neto = BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        }

        return new ResultadoNeto(comision, neto);
    }

    /**
     * Igual que {@link #calcular(BigDecimal)} pero ante cualquier error devuelve comisión cero y neto = bruto (comportamiento previo al paso 1).
     */
    public ResultadoNeto calcularConFallbackSinComision(BigDecimal montoBruto) {
        try {
            return calcular(montoBruto);
        } catch (RuntimeException e) {
            log.warn("NetoTransaccionService: fallo al calcular netos; se usa sin comisión (fallback)", e);
            BigDecimal bruto = montoBruto != null ? montoBruto.setScale(SCALE_MONEDA, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(SCALE_MONEDA);
            BigDecimal z = BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
            return new ResultadoNeto(z, bruto);
        }
    }

    public record ResultadoNeto(BigDecimal comisionApp, BigDecimal montoNeto) {
    }
}
