package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;

/**
 * Desglose monetario por reserva: cobro bruto al tutor, comisión de plataforma y neto del paseador.
 */
public record DesgloseComisionResponse(
        BigDecimal montoBruto,
        BigDecimal comision,
        BigDecimal montoNeto
) {
}
