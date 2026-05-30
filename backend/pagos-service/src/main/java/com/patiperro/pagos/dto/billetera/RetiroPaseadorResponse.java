package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;

public record RetiroPaseadorResponse(
        Long idTransaccion,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponibleTrasRetiro,
        String mensaje
) {
}
