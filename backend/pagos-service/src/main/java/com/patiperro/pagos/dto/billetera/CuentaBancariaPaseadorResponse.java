package com.patiperro.pagos.dto.billetera;

/**
 * Resumen de la cuenta bancaria asociada a la billetera del paseador (sin exponer el número completo).
 */
public record CuentaBancariaPaseadorResponse(
        String id,
        String bankName,
        String accountType,
        String accountNumberMasked,
        String holderName) {
}
