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
 * <p>
 * La comprobación {@link #montosCierranIntegridad} / {@link #advertirSiMontosNoIntegran} solo registra incoherencias;
 * no interrumpe el flujo de pago (evita dejar approved sin persistir por un guardarraíl demasiado estricto).
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NetoTransaccionService {

    private static final Logger log = LoggerFactory.getLogger(NetoTransaccionService.class);

    private static final int SCALE_MONEDA = 2;

    private static final int SCALE_TASA_EXPOSICION = 6;

    private final ComisionPlataformaProperties comisionPlataformaProperties;

    /**
     * Fracción de comisión vigente [0, 1] para respuestas API (misma acotación que {@link #calcular(BigDecimal)}).
     */
    public BigDecimal tasaPlataformaFraccionParaExposicion() {
        return tasaInternaAcotada().setScale(SCALE_TASA_EXPOSICION, RoundingMode.HALF_UP);
    }

    private BigDecimal tasaInternaAcotada() {
        BigDecimal tasa = comisionPlataformaProperties.getPlataformaTasa();
        if (tasa == null) {
            tasa = BigDecimal.ZERO;
        }
        return tasa.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

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

        BigDecimal tasa = tasaInternaAcotada();

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

    /**
     * {@code montoBruto = montoNeto + comisionApp} en escala monetaria (2 decimales). Sin tolerancia: incoherencias
     * solo pueden venir de datos corruptos o campos pisados fuera de este servicio.
     */
    public boolean montosCierranIntegridad(BigDecimal montoBruto, BigDecimal comisionApp, BigDecimal montoNeto) {
        BigDecimal bruto = nzScale(montoBruto);
        BigDecimal com = nzScale(comisionApp);
        BigDecimal net = nzScale(montoNeto);
        BigDecimal suma = net.add(com).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        return bruto.compareTo(suma) == 0;
    }

    /**
     * Observabilidad ante inconsistencia; no lanza.
     */
    public void advertirSiMontosNoIntegran(
            BigDecimal montoBruto, BigDecimal comisionApp, BigDecimal montoNeto, String contexto) {
        if (montosCierranIntegridad(montoBruto, comisionApp, montoNeto)) {
            return;
        }
        BigDecimal bruto = nzScale(montoBruto);
        BigDecimal com = nzScale(comisionApp);
        BigDecimal net = nzScale(montoNeto);
        BigDecimal suma = net.add(com).setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        BigDecimal diff = bruto.subtract(suma).abs().setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        log.warn(
                "NetoTransaccionService: montos no cierran (bruto={} comision={} neto={} sumaNetoMasComision={} diff={}) contexto={}",
                bruto,
                com,
                net,
                suma,
                diff,
                contexto != null ? contexto : "");
    }

    private static BigDecimal nzScale(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
    }

    public record ResultadoNeto(BigDecimal comisionApp, BigDecimal montoNeto) {
    }
}
